package br.com.conecta21.api.service;

import br.com.conecta21.api.dto.ChamadoCriacaoDTO;
import br.com.conecta21.api.dto.ChamadoRespostaDTO;
import br.com.conecta21.api.dto.ChamadoStatusDTO;
import br.com.conecta21.api.model.Chamado;
import br.com.conecta21.api.model.StatusChamado;
import br.com.conecta21.api.model.Usuario;
import br.com.conecta21.api.repository.ChamadoRepository;
import br.com.conecta21.api.repository.ChamadoSpecs;
import br.com.conecta21.api.repository.EmpresaRepository;
import br.com.conecta21.api.repository.UsuarioRepository;
import br.com.conecta21.api.security.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Motor operacional de chamados (Backend C — Sprint 1 e Motor SLA — Sprint 3).
 *
 * <p>Validação estrita de tenant: todo acesso puxa o {@code empresa_id} do
 * token JWT (via {@link TenantContext#getEmpresaIdAutenticada()}, com validação
 * cruzada contra o banco) e aplica esse ID diretamente nas consultas JPA.
 * Nenhum {@code empresaId} vindo do body/query é aceito.
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

    @Transactional
    public ChamadoRespostaDTO criar(ChamadoCriacaoDTO dto) {
        // Fonte do tenant: claim empresa_id do JWT (TenantContext já valida contra o banco).
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

        return toResposta(chamadoRepository.save(chamado));
    }

    @Transactional(readOnly = true)
    public Page<ChamadoRespostaDTO> listar(
            String statusFiltro, Long tecnicoId,
            LocalDateTime dataInicio, LocalDateTime dataFim,
            Pageable pageable) {
        Long empresaId = tenantContext.getEmpresaIdAutenticada();

        StatusChamado status = null;
        if (statusFiltro != null && !statusFiltro.isBlank()) {
            try {
                status = StatusChamado.valueOf(statusFiltro.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Status inválido. Valores aceitos: ABERTO, EM_ANDAMENTO, RESOLVIDO, EM_ATRASO");
            }
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
        StatusChamado novoStatus;

        try {
            novoStatus = StatusChamado.valueOf(dto.status().trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Status inválido. Valores aceitos: ABERTO, EM_ANDAMENTO, RESOLVIDO, EM_ATRASO");
        }

        Chamado chamado = buscarNoTenant(id);
        chamado.setStatus(novoStatus);

        if (StatusChamado.RESOLVIDO.equals(novoStatus)) {
            if (chamado.getDataFechamento() == null) {
                chamado.setDataFechamento(LocalDateTime.now());
            }
        } else {
            chamado.setDataFechamento(null);
        }

        return toResposta(chamado);
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
                chamado.getStatus().name(), // Converte o Enum de volta para String no JSON de resposta
                chamado.getSolicitante().getId(),
                chamado.getTecnico() != null ? chamado.getTecnico().getId() : null,
                chamado.getDataAbertura(),
                chamado.getDataFechamento());
    }
}