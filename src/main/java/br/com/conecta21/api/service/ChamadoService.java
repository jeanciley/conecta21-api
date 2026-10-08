package br.com.conecta21.api.service;

import br.com.conecta21.api.aop.AuditarAcao;
import br.com.conecta21.api.dto.ChamadoCriacaoDTO;
import br.com.conecta21.api.dto.ChamadoRespostaDTO;
import br.com.conecta21.api.dto.ChamadoStatusDTO;
import br.com.conecta21.api.dto.ChamadoResponsavelAlteracaoDTO;
import br.com.conecta21.api.dto.ResponsavelChamadoDTO;
import br.com.conecta21.api.model.Chamado;
import br.com.conecta21.api.model.Categoria;
import br.com.conecta21.api.model.PerfilUsuario;
import br.com.conecta21.api.model.StatusChamado;
import br.com.conecta21.api.model.TipoChamado;
import br.com.conecta21.api.model.Usuario;
import br.com.conecta21.api.model.InteracaoChamado;
import br.com.conecta21.api.dto.KanbanCardDTO;
import br.com.conecta21.api.dto.KanbanResponseDTO;
import br.com.conecta21.api.repository.ChamadoRepository;
import br.com.conecta21.api.repository.ChamadoSpecs;
import br.com.conecta21.api.repository.CategoriaRepository;
import br.com.conecta21.api.model.Prioridade;
import br.com.conecta21.api.repository.EmpresaRepository;
import br.com.conecta21.api.repository.UsuarioRepository;
import br.com.conecta21.api.repository.InteracaoChamadoRepository;
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
import java.util.Comparator;

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
    private InteracaoChamadoRepository interacaoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private TenantContext tenantContext;

    @Autowired
    private AnexoChamadoService anexoChamadoService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Transactional
    @CacheEvict(value = "kanban_empresa", allEntries = true)
    @AuditarAcao(acao = "CRIACAO", entidade = "Chamado")
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
        TipoChamado tipoChamado = dto.tipo() != null ? dto.tipo()
                : (dto.interno() ? TipoChamado.TI_INTERNO : TipoChamado.SUPORTE_EXTERNO);
        if (dto.interno() && tipoChamado == TipoChamado.SUPORTE_EXTERNO) {
            tipoChamado = TipoChamado.TI_INTERNO;
        }
        boolean interno = dto.interno() || tipoChamado != TipoChamado.SUPORTE_EXTERNO;
        if (interno && !solicitante.temPermissao("CHAMADOS_INTERNOS")) {
            throw new AccessDeniedException("Sem permissÃ£o para abrir chamados internos.");
        }
        if (!interno && solicitante.getPerfil() == PerfilUsuario.USUARIO
                && solicitante.temPermissao("CHAMADOS_INTERNOS")
                && !solicitante.temPermissao("GERENCIAR_CHAMADOS")) {
            throw new AccessDeniedException("Este perfil pode abrir somente chamados internos.");
        }
        chamado.setInterno(interno);
        chamado.setTipo(tipoChamado);
        Categoria categoria = categoriaRepository.findByIdAndEmpresaId(dto.categoriaId(), empresaId)
                .filter(Categoria::isAtiva)
                .orElseThrow(() -> new IllegalArgumentException("Categoria ativa não encontrada."));
        Prioridade prioridade = categoria.getPrioridade();
        if (prioridade == null || !prioridade.isAtiva()) {
            throw new IllegalArgumentException("A categoria não possui prioridade ativa configurada.");
        }
        chamado.setCategoria(categoria);
        chamado.setPrioridadeConfigurada(prioridade);
        chamado.setPrioridade(prioridade.getNome());
        chamado.setSlaRespostaMinutosSnapshot(prioridade.getSlaRespostaMinutos());
        chamado.setSlaResolucaoMinutosSnapshot(prioridade.getSlaResolucaoMinutos());

        chamado.setTecnico(selecionarTecnicoComMenorCarga(empresaId));

        Chamado salvo = chamadoRepository.save(chamado);
        anexoChamadoService.salvarAnexos(salvo, arquivos);
        return toResposta(salvo, deveOcultarPrioridade());
    }

    @Transactional(readOnly = true)
    public Page<ChamadoRespostaDTO> listar(
            String statusFiltro, Long tecnicoId,
            LocalDateTime dataInicio, LocalDateTime dataFim,
            Pageable pageable) {
        return listar(statusFiltro, tecnicoId, dataInicio, dataFim, false, pageable);
    }

    @Transactional(readOnly = true)
    public Page<ChamadoRespostaDTO> listar(
            String statusFiltro, Long tecnicoId,
            LocalDateTime dataInicio, LocalDateTime dataFim, boolean interno,
            Pageable pageable) {
        return listar(statusFiltro, tecnicoId, dataInicio, dataFim, interno, null, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<ChamadoRespostaDTO> listar(
            String statusFiltro, Long tecnicoId,
            LocalDateTime dataInicio, LocalDateTime dataFim, boolean interno,
            String busca, List<String> tiposFiltro, Pageable pageable) {
        Long empresaId = tenantContext.getEmpresaIdAutenticada();
        Usuario usuario = tenantContext.getUsuarioAutenticado();
        exigirTipoChamadoPermitido(usuario, interno);
        StatusChamado status = converterStatusOpcional(statusFiltro);
        List<TipoChamado> tipos = converterTipos(tiposFiltro);
        boolean ocultarPrioridade = deveOcultarPrioridade();
        Long solicitanteId = podeGerenciarChamados(usuario) ? null : usuario.getId();
        return chamadoRepository
                .findAll(ChamadoSpecs.noTenantComFiltros(empresaId, status, tecnicoId, dataInicio, dataFim, interno, tipos, busca, solicitanteId), pageable)
                .map(chamado -> toResposta(chamado, ocultarPrioridade));
    }

    @Transactional(readOnly = true)
    public ChamadoRespostaDTO detalhar(Long id) {
        boolean ocultarPrioridade = deveOcultarPrioridade();
        Chamado chamado = buscarNoTenant(id);
        exigirAcessoInterno(chamado);
        return toResposta(chamado, ocultarPrioridade);
    }

    // A chave do cache agora junta o ID da empresa com os tipos solicitados
    @Transactional(readOnly = true)
    public KanbanResponseDTO obterKanban(Long tecnicoId, LocalDateTime dataInicio,
                                          LocalDateTime dataFim, Integer limite) {
        return obterKanban(tecnicoId, dataInicio, dataFim, limite, null, false);
    }

    @Cacheable(value = "kanban_empresa", key = "@tenantContext.getEmpresaIdAutenticada() + '-' + @tenantContext.getUsuarioAutenticado().getId() + '-' + #tecnicoId + '-' + #dataInicio + '-' + #dataFim + '-' + #limite + '-' + #tiposFiltro + '-' + #interno")
    @Transactional(readOnly = true)
    public KanbanResponseDTO obterKanban(
            Long tecnicoId,
            LocalDateTime dataInicio, LocalDateTime dataFim,
            Integer limite,
            List<String> tiposFiltro,
            boolean interno) {

        Long empresaId = tenantContext.getEmpresaIdAutenticada();
        Usuario usuario = tenantContext.getUsuarioAutenticado();
        if (interno && !usuario.temPermissao("CHAMADOS_INTERNOS")) {
            throw new AccessDeniedException("Sem permissÃ£o para consultar chamados internos.");
        }
        List<TipoChamado> tipos = converterTipos(tiposFiltro);

        int porColuna = limite != null ? limite : LIMITE_KANBAN_PADRAO;
        if (porColuna < 1 || porColuna > LIMITE_KANBAN_MAXIMO) {
            throw new IllegalArgumentException("Limite inválido.");
        }

        Pageable paginaColuna = PageRequest.of(0, porColuna, Sort.by(Sort.Direction.DESC, "dataAbertura"));

        return new KanbanResponseDTO(
                buscarColuna(empresaId, StatusChamado.ABERTO, tecnicoId, dataInicio, dataFim, tipos, interno, paginaColuna),
                buscarColuna(empresaId, StatusChamado.EM_ANDAMENTO, tecnicoId, dataInicio, dataFim, tipos, interno, paginaColuna),
                buscarColuna(empresaId, StatusChamado.EM_ATRASO, tecnicoId, dataInicio, dataFim, tipos, interno, paginaColuna),
                buscarColuna(empresaId, StatusChamado.RESOLVIDO, tecnicoId, dataInicio, dataFim, tipos, interno, paginaColuna));
    }

    @CacheEvict(value = "kanban_empresa", allEntries = true)
    @AuditarAcao(acao = "ALTERACAO_STATUS", entidade = "Chamado")
    @Transactional
    public ChamadoRespostaDTO alterarStatus(Long id, ChamadoStatusDTO dto) {
        StatusChamado novoStatus = converterStatusObrigatorio(dto.status());
        Chamado chamado = buscarNoTenant(id);
        exigirAcessoInterno(chamado);
        if (!podeGerenciarChamados(tenantContext.getUsuarioAutenticado())) {
            throw new AccessDeniedException("Somente a equipe de atendimento pode alterar o status do chamado.");
        }
        StatusChamado statusAnterior = chamado.getStatus();

        if (statusAnterior == novoStatus) {
            return toResposta(chamado, deveOcultarPrioridade());
        }

        // CORREÇÃO: Lógica duplicada de buscarNoTenant e verificação de perfil removidas.
        if (StatusChamado.RESOLVIDO.equals(novoStatus)) {
            Usuario ator = tenantContext.getUsuarioAutenticado();
            if (!ator.temPermissao("GERENCIAR_CHAMADOS") && ator.getPerfil() != PerfilUsuario.TECNICO) {
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

        return toResposta(chamado, deveOcultarPrioridade());
    }

    @Transactional(readOnly = true)
    public List<ResponsavelChamadoDTO> listarResponsaveis(Long id) {
        Usuario ator = tenantContext.getUsuarioAutenticado();
        if (!podeGerenciarChamados(ator)) {
            throw new AccessDeniedException("Somente técnicos e administradores podem repassar chamados.");
        }
        Chamado chamado = buscarNoTenant(id);
        exigirAcessoInterno(chamado);
        return listarTecnicosAtivos(chamado.getEmpresa().getId()).stream()
                .map(usuario -> new ResponsavelChamadoDTO(usuario.getId(), usuario.getNome(), usuario.getPerfil().name()))
                .toList();
    }

    @Transactional
    @CacheEvict(value = "kanban_empresa", allEntries = true)
    @AuditarAcao(acao = "TRANSFERENCIA_RESPONSAVEL", entidade = "Chamado")
    public ChamadoRespostaDTO transferirResponsavel(Long id, ChamadoResponsavelAlteracaoDTO dto) {
        Usuario ator = tenantContext.getUsuarioAutenticado();
        if (!podeGerenciarChamados(ator)) {
            throw new AccessDeniedException("Somente técnicos e administradores podem repassar chamados.");
        }
        Chamado chamado = buscarNoTenant(id);
        exigirAcessoInterno(chamado);

        Usuario novoResponsavel = listarTecnicosAtivos(chamado.getEmpresa().getId()).stream()
                .filter(usuario -> usuario.getId().equals(dto.responsavelId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("O novo responsável não é um técnico ativo desta empresa."));
        Usuario responsavelAnterior = chamado.getTecnico();
        if (responsavelAnterior != null && responsavelAnterior.getId().equals(novoResponsavel.getId())) {
            return toResposta(chamado, deveOcultarPrioridade());
        }

        chamado.setTecnico(novoResponsavel);
        InteracaoChamado historico = new InteracaoChamado();
        historico.setChamado(chamado);
        historico.setAutor(usuarioRepository.getReferenceById(ator.getId()));
        historico.setTipo("Transferência");
        String origem = responsavelAnterior == null ? "Sem responsável" : responsavelAnterior.getNome();
        historico.setMensagem("Chamado repassado de " + origem + " para " + novoResponsavel.getNome() + ".");
        interacaoRepository.save(historico);

        return toResposta(chamado, deveOcultarPrioridade());
    }

    private Usuario selecionarTecnicoComMenorCarga(Long empresaId) {
        List<StatusChamado> emAtendimento = List.of(
                StatusChamado.ABERTO, StatusChamado.EM_ANDAMENTO, StatusChamado.EM_ATRASO);
        List<Usuario> tecnicos = usuarioRepository.findAllByEmpresaIdAndPerfilInAndAtivoTrueOrderByNomeAsc(
                empresaId, List.of(PerfilUsuario.TECNICO));
        return tecnicos.stream()
                .min(Comparator.comparingLong((Usuario tecnico) ->
                                chamadoRepository.countByEmpresaIdAndTecnicoIdAndStatusIn(empresaId, tecnico.getId(), emAtendimento))
                        .thenComparing(Usuario::getId))
                .orElseThrow(() -> new IllegalStateException("Não há técnicos ativos para receber novos chamados."));
    }

    private List<Usuario> listarTecnicosAtivos(Long empresaId) {
        return usuarioRepository.findAllByEmpresaIdAndPerfilInAndAtivoTrueOrderByNomeAsc(
                empresaId, List.of(PerfilUsuario.TECNICO, PerfilUsuario.ADMIN));
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

    private boolean deveOcultarPrioridade() {
        Usuario usuario = tenantContext.getUsuarioAutenticado();
        return usuario != null && usuario.getPerfil() == PerfilUsuario.USUARIO;
    }

    public void exigirAcessoInterno(Chamado chamado) {
        Usuario usuario = tenantContext.getUsuarioAutenticado();
        if (usuario.getPerfil() == PerfilUsuario.USUARIO
                && chamado.isInterno() != usuario.temPermissao("CHAMADOS_INTERNOS")) {
            throw new EntityNotFoundException("Chamado não encontrado.");
        }
        if (!podeGerenciarChamados(usuario) && !chamado.getSolicitante().getId().equals(usuario.getId())) {
            throw new EntityNotFoundException("Chamado não encontrado.");
        }
    }

    private void exigirTipoChamadoPermitido(Usuario usuario, boolean interno) {
        if (usuario.getPerfil() == PerfilUsuario.USUARIO
                && interno != usuario.temPermissao("CHAMADOS_INTERNOS")) {
            throw new AccessDeniedException("Seu perfil não possui acesso a este tipo de chamado.");
        }
        if (interno && !usuario.temPermissao("CHAMADOS_INTERNOS")) {
            throw new AccessDeniedException("Sem permissão para consultar chamados internos.");
        }
    }

    private boolean podeGerenciarChamados(Usuario usuario) {
        return usuario.getPerfil() == PerfilUsuario.ADMIN || usuario.getPerfil() == PerfilUsuario.TECNICO
                || usuario.temPermissao("GERENCIAR_CHAMADOS");
    }

    private List<TipoChamado> converterTipos(List<String> tiposFiltro) {
        if (tiposFiltro == null || tiposFiltro.isEmpty()) return List.of();
        try {
            return tiposFiltro.stream().filter(tipo -> tipo != null && !tipo.isBlank())
                    .map(tipo -> TipoChamado.valueOf(tipo.trim().toUpperCase(java.util.Locale.ROOT)))
                    .distinct().toList();
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Tipo de chamado inválido. Valores aceitos: SUPORTE_EXTERNO, TI_INTERNO, FACILITIES.");
        }
    }

    private List<KanbanCardDTO> buscarColuna(Long empresaId, StatusChamado status, Long tecnicoId,
                                              LocalDateTime dataInicio, LocalDateTime dataFim,
                                              List<TipoChamado> tipos, boolean interno, Pageable pageable) {
        boolean ocultarPrioridade = deveOcultarPrioridade();
        return chamadoRepository.findAll(
                        ChamadoSpecs.noTenantComFiltros(empresaId, status, tecnicoId, dataInicio, dataFim, interno, tipos, null,
                                podeGerenciarChamados(tenantContext.getUsuarioAutenticado()) ? null : tenantContext.getUsuarioAutenticado().getId()),
                        pageable)
                .map(chamado -> new KanbanCardDTO(
                        chamado.getId(), chamado.getTitulo(), ocultarPrioridade ? null : chamado.getPrioridade(), chamado.getStatus().name(),
                        chamado.getTipo() == null ? null : chamado.getTipo().name(), chamado.getSolicitante().getId(),
                        chamado.getTecnico() == null ? null : chamado.getTecnico().getId(),
                        chamado.getDataAbertura(), chamado.getDataLimiteResolucao()))
                .getContent();
    }

    private ChamadoRespostaDTO toResposta(Chamado chamado, boolean ocultarPrioridade) {
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
                ocultarPrioridade ? null : chamado.getPrioridade(),
                chamado.getDataLimiteResolucao(),
                chamado.getSolicitante().getNome(),
                chamado.getTecnico() != null ? chamado.getTecnico().getNome() : null,
                chamado.getCategoria() != null ? chamado.getCategoria().getId() : null,
                chamado.getCategoria() != null ? chamado.getCategoria().getNome() : null,
                chamado.getDataLimiteResposta());
    }
}
