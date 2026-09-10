package br.com.conecta21.api.service;

import br.com.conecta21.api.dto.ChamadoCriacaoDTO;
import br.com.conecta21.api.dto.ChamadoRespostaDTO;
import br.com.conecta21.api.dto.ChamadoStatusDTO;
import br.com.conecta21.api.model.Chamado;
import br.com.conecta21.api.model.StatusChamado;
import br.com.conecta21.api.model.Usuario;
import br.com.conecta21.api.repository.ChamadoRepository;
import br.com.conecta21.api.repository.EmpresaRepository;
import br.com.conecta21.api.repository.UsuarioRepository;
import br.com.conecta21.api.security.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Motor operacional de chamados (Backend C — Sprint 1 e Motor SLA — Sprint 3).
 *
 * <p>Todo acesso é escopado pela empresa do usuário autenticado, com o
 * {@code empresa_id} aplicado diretamente nas consultas JPA.
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
        Usuario solicitante = tenantContext.getUsuarioAutenticado();
        Long empresaId = solicitante.getEmpresa().getId();

        Chamado chamado = new Chamado();
        chamado.setEmpresa(empresaRepository.getReferenceById(empresaId));
        chamado.setSolicitante(usuarioRepository.getReferenceById(solicitante.getId()));
        chamado.setTitulo(dto.titulo());
        chamado.setDescricao(dto.descricao());

        chamado.setPrioridade(dto.prioridade());

        return toResposta(chamadoRepository.save(chamado));
    }

    @Transactional(readOnly = true)
    public List<ChamadoRespostaDTO> listar() {
        return chamadoRepository.findAllByEmpresaId(tenantContext.getEmpresaIdAutenticada())
                .stream()
                .map(this::toResposta)
                .toList();
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