package br.com.conecta21.api.service;

import br.com.conecta21.api.aop.AuditarAcao;
import br.com.conecta21.api.dto.ChamadoCriacaoDTO;
import br.com.conecta21.api.dto.ChamadoRespostaDTO;
import br.com.conecta21.api.dto.ChamadoStatusDTO;
import br.com.conecta21.api.dto.KanbanCardDTO;
import br.com.conecta21.api.dto.KanbanResponseDTO;
import br.com.conecta21.api.model.*;
import br.com.conecta21.api.repository.ChamadoRepository;
import br.com.conecta21.api.repository.ChamadoSpecs;
import br.com.conecta21.api.repository.EmpresaRepository;
import br.com.conecta21.api.repository.UsuarioRepository;
import br.com.conecta21.api.security.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Motor operacional de chamados.
 *
 * <p>Todo acesso mantém a trava multi-tenant baseada no empresa_id do JWT,
 * validado pelo {@link TenantContext}. Nenhum empresaId recebido do cliente
 * é usado para autorizar acesso.</p>
 */
@Service
public class ChamadoService {

    private static final int LIMITE_KANBAN_PADRAO = 50;
    private static final int LIMITE_KANBAN_MAXIMO = 200;

    @Autowired
    private ChamadoRepository chamadoRepository;

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TenantContext tenantContext;

    @Autowired
    private AnexoChamadoService anexoChamadoService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Transactional
    public ChamadoRespostaDTO criar(ChamadoCriacaoDTO dto) {
        return criarInterno(dto, List.of());
    }

    @CacheEvict(value = "kanban_empresa", allEntries = true) // Limpa todos os quadros kanban quando há alteração
    @AuditarAcao(acao = "CRIACAO", entidade = "Chamado")
    @Transactional
    public ChamadoRespostaDTO criar(ChamadoCriacaoDTO dto, List<MultipartFile> arquivos) {
        return criarInterno(dto, arquivos == null ? List.of() : arquivos);
    }

    private ChamadoRespostaDTO criarInterno(ChamadoCriacaoDTO dto, List<MultipartFile> arquivos) {
        Long empresaId = tenantContext.getEmpresaIdAutenticada();
        Usuario solicitante = tenantContext.getUsuarioAutenticado();

        if (!empresaId.equals(solicitante.getEmpresa().getId())) {
            throw new AccessDeniedException("Divergência de tenant entre token e usuário.");
        }

        Chamado chamado = new Chamado();
        chamado.setEmpresa(empresaRepository.getReferenceById(empresaId));
        chamado.setSolicitante(usuarioRepository.getReferenceById(solicitante.getId()));
        chamado.setTitulo(dto.titulo());
        chamado.setDescricao(dto.descricao());
        chamado.setStatus(StatusChamado.ABERTO);

        // Mantém a lógica de prioridade que você já possui
        if (dto.prioridade() != null && !dto.prioridade().isBlank()) {
            chamado.setPrioridade(PrioridadeChamado.valueOf(dto.prioridade().trim().toUpperCase()));
        }

        // NOVA LÓGICA: Roteamento de Tipo de Chamado
        if (dto.tipo() != null && !dto.tipo().isBlank()) {
            try {
                chamado.setTipo(TipoChamado.valueOf(dto.tipo().trim().toUpperCase()));
            } catch (IllegalArgumentException e) {
                // Se o front-end mandar um tipo que não existe, força para o padrão
                chamado.setTipo(TipoChamado.SUPORTE_EXTERNO);
            }
        } else {
            chamado.setTipo(TipoChamado.SUPORTE_EXTERNO);
        }

        Chamado salvo = chamadoRepository.save(chamado);
        anexoChamadoService.salvarAnexos(salvo, arquivos);
        return toResposta(salvo);
    }

    @Transactional(readOnly = true)
    public Page<ChamadoRespostaDTO> listar(
            String statusFiltro, Long tecnicoId,
            LocalDateTime dataInicio, LocalDateTime dataFim,
            Pageable pageable) {
        return listar(statusFiltro, tecnicoId, dataInicio, dataFim, null, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<ChamadoRespostaDTO> listar(
            String statusFiltro, Long tecnicoId,
            LocalDateTime dataInicio, LocalDateTime dataFim,
            String busca, List<String> tiposFiltro, Pageable pageable) { // Adicionado o tiposFiltro

        Long empresaId = tenantContext.getEmpresaIdAutenticada();
        StatusChamado status = converterStatusOpcional(statusFiltro);
        List<TipoChamado> tipos = converterTipos(tiposFiltro); // Converte os tipos enviados

        if (busca != null && !busca.isBlank()) {
            Pageable paginaSemOrdenacaoExterna = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
            return chamadoRepository
                    .pesquisarFullText(
                            empresaId,
                            busca.trim(),
                            status != null ? status.name() : null,
                            tecnicoId,
                            dataInicio,
                            dataFim,
                            paginaSemOrdenacaoExterna) // NOTA: Se você for usar tipos na busca FullText, precisará alterar a query nativa do repositório também!
                    .map(this::toResposta);
        }

        return chamadoRepository
                .findAll(ChamadoSpecs.noTenantComFiltros(empresaId, status, tecnicoId, dataInicio, dataFim, tipos), pageable)
                .map(this::toResposta);
    }

    @Transactional(readOnly = true)
    public ChamadoRespostaDTO detalhar(Long id) {
        return toResposta(buscarNoTenant(id));
    }

    // A chave do cache agora junta o ID da empresa com os tipos solicitados
    @Cacheable(value = "kanban_empresa", key = "#{@tenantContext.getEmpresaIdAutenticada()} + '-' + (#tiposFiltro != null ? #tiposFiltro.toString() : 'TODOS')")
    @Transactional(readOnly = true)
    public KanbanResponseDTO obterKanban(
            Long tecnicoId,
            LocalDateTime dataInicio, LocalDateTime dataFim,
            Integer limite,
            List<String> tiposFiltro) { // Novo parâmetro recebido do Controller

        Long empresaId = tenantContext.getEmpresaIdAutenticada();
        List<TipoChamado> tipos = converterTipos(tiposFiltro);

        int porColuna = limite != null ? limite : LIMITE_KANBAN_PADRAO;
        if (porColuna < 1 || porColuna > LIMITE_KANBAN_MAXIMO) {
            throw new IllegalArgumentException("Limite inválido.");
        }

        Pageable paginaColuna = PageRequest.of(0, porColuna, Sort.by(Sort.Direction.DESC, "dataAbertura"));

        return new KanbanResponseDTO(
                buscarColuna(empresaId, StatusChamado.ABERTO, tecnicoId, dataInicio, dataFim, tipos, paginaColuna),
                buscarColuna(empresaId, StatusChamado.EM_ANDAMENTO, tecnicoId, dataInicio, dataFim, tipos, paginaColuna),
                buscarColuna(empresaId, StatusChamado.EM_ATRASO, tecnicoId, dataInicio, dataFim, tipos, paginaColuna),
                buscarColuna(empresaId, StatusChamado.RESOLVIDO, tecnicoId, dataInicio, dataFim, tipos, paginaColuna));
    }

    @CacheEvict(value = "kanban_empresa", key = "#{@tenantContext.getEmpresaIdAutenticada()}")
    @AuditarAcao(acao = "ALTERACAO_STATUS", entidade = "Chamado")
    @Transactional
    public ChamadoRespostaDTO alterarStatus(Long id, ChamadoStatusDTO dto) {
        StatusChamado novoStatus = converterStatusObrigatorio(dto.status());
        Chamado chamado = buscarNoTenant(id);
        StatusChamado statusAnterior = chamado.getStatus();

        if (statusAnterior == novoStatus) {
            return toResposta(chamado);
        }

        // CORREÇÃO: Lógica duplicada de buscarNoTenant e verificação de perfil removidas.
        if (StatusChamado.RESOLVIDO.equals(novoStatus)) {
            Usuario ator = tenantContext.getUsuarioAutenticado();
            if (ator.getPerfil() != PerfilUsuario.TECNICO && ator.getPerfil() != PerfilUsuario.ADMIN) {
                throw new AccessDeniedException("Apenas técnico ou administrador pode marcar o chamado como RESOLVIDO.");
            }
        }

        chamado.setStatus(novoStatus);

        if (StatusChamado.RESOLVIDO.equals(novoStatus)) {
            if (chamado.getDataFechamento() == null) {
                chamado.setDataFechamento(LocalDateTime.now());
            }
        } else {
            chamado.setDataFechamento(null);
        }

        publicarAlteracaoStatus(chamado, statusAnterior, novoStatus);
        return toResposta(chamado);
    }

    private void publicarAlteracaoStatus(Chamado chamado, StatusChamado anterior, StatusChamado novo) {
        eventPublisher.publishEvent(new ChamadoStatusAlteradoEvent(
                chamado.getId(),
                chamado.getTitulo(),
                chamado.getSolicitante().getEmail(),
                chamado.getSolicitante().getNome(),
                anterior,
                novo));
    }

    private StatusChamado converterStatusOpcional(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return converterStatusObrigatorio(status);
    }

    private StatusChamado converterStatusObrigatorio(String status) {
        try {
            return StatusChamado.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException(
                    "Status inválido. Valores aceitos: ABERTO, EM_ANDAMENTO, RESOLVIDO, EM_ATRASO");
        }
    }

    private Chamado buscarNoTenant(Long id) {
        return chamadoRepository.findByIdAndEmpresaId(id, tenantContext.getEmpresaIdAutenticada())
                .orElseThrow(() -> new EntityNotFoundException("Chamado não encontrado."));
    }

    private List<KanbanCardDTO> buscarColuna(
            Long empresaId, StatusChamado status, Long tecnicoId,
            LocalDateTime dataInicio, LocalDateTime dataFim,
            List<TipoChamado> tipos, // Novo parâmetro
            Pageable paginaColuna) {

        return chamadoRepository
                .findAll(ChamadoSpecs.noTenantComFiltros(empresaId, status, tecnicoId, dataInicio, dataFim, tipos), paginaColuna)
                .map(this::toCard)
                .getContent();
    }

    private KanbanCardDTO toCard(Chamado chamado) {
        return new KanbanCardDTO(
                chamado.getId(),
                chamado.getTitulo(),
                chamado.getPrioridade() != null ? chamado.getPrioridade().name() : null,
                chamado.getStatus().name(),
                chamado.getSolicitante().getId(),
                chamado.getTecnico() != null ? chamado.getTecnico().getId() : null,
                chamado.getDataAbertura(),
                chamado.getDataLimiteResolucao());
    }

    private ChamadoRespostaDTO toResposta(Chamado chamado) {
        return new ChamadoRespostaDTO(
                chamado.getId(),
                chamado.getEmpresa().getId(),
                chamado.getTitulo(),
                chamado.getDescricao(),
                chamado.getStatus().name(),
                chamado.getSolicitante().getId(),
                chamado.getTecnico() != null ? chamado.getTecnico().getId() : null,
                chamado.getDataAbertura(),
                chamado.getDataFechamento(),
                chamado.getTipo().name());
    }

    private List<TipoChamado> converterTipos(List<String> tipos) {
        if (tipos == null || tipos.isEmpty()) {
            return null;
        }
        return tipos.stream()
                .map(t -> {
                    try {
                        return TipoChamado.valueOf(t.trim().toUpperCase());
                    } catch (IllegalArgumentException e) {
                        return null; // Ignora tipos inválidos enviados na URL
                    }
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }
}