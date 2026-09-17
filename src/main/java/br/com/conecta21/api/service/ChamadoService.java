package br.com.conecta21.api.service;

import br.com.conecta21.api.dto.ChamadoCriacaoDTO;
import br.com.conecta21.api.dto.ChamadoRespostaDTO;
import br.com.conecta21.api.dto.ChamadoStatusDTO;
import br.com.conecta21.api.model.Chamado;
import br.com.conecta21.api.model.PerfilUsuario;
import br.com.conecta21.api.model.StatusChamado;
import br.com.conecta21.api.model.Usuario;
import br.com.conecta21.api.repository.ChamadoRepository;
import br.com.conecta21.api.repository.ChamadoSpecs;
import br.com.conecta21.api.repository.EmpresaRepository;
import br.com.conecta21.api.repository.UsuarioRepository;
import br.com.conecta21.api.security.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
        chamado.setPrioridade(dto.prioridade());

        Chamado salvo = chamadoRepository.save(chamado);
        anexoChamadoService.salvarAnexos(salvo, arquivos);
        return toResposta(salvo);
    }

    @Transactional(readOnly = true)
    public Page<ChamadoRespostaDTO> listar(
            String statusFiltro, Long tecnicoId,
            LocalDateTime dataInicio, LocalDateTime dataFim,
            Pageable pageable) {
        return listar(statusFiltro, tecnicoId, dataInicio, dataFim, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<ChamadoRespostaDTO> listar(
            String statusFiltro, Long tecnicoId,
            LocalDateTime dataInicio, LocalDateTime dataFim,
            String busca, Pageable pageable) {
        Long empresaId = tenantContext.getEmpresaIdAutenticada();
        StatusChamado status = converterStatusOpcional(statusFiltro);

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
                            paginaSemOrdenacaoExterna)
                    .map(this::toResposta);
        }

        return chamadoRepository
                .findAll(ChamadoSpecs.noTenantComFiltros(empresaId, status, tecnicoId, dataInicio, dataFim), pageable)
                .map(this::toResposta);
    }

    @Transactional(readOnly = true)
    public ChamadoRespostaDTO detalhar(Long id) {
        return toResposta(buscarNoTenant(id));
    }

    @Transactional
    public ChamadoRespostaDTO alterarStatus(Long id, ChamadoStatusDTO dto) {
        StatusChamado novoStatus = converterStatusObrigatorio(dto.status());
        Chamado chamado = buscarNoTenant(id);
        StatusChamado statusAnterior = chamado.getStatus();

        if (statusAnterior == novoStatus) {
            return toResposta(chamado);
        }

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
                chamado.getDataFechamento());
    }
}
