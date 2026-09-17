package br.com.conecta21.api.service;

import br.com.conecta21.api.dto.AvaliacaoCriacaoDTO;
import br.com.conecta21.api.dto.AvaliacaoRespostaDTO;
import br.com.conecta21.api.model.Avaliacao;
import br.com.conecta21.api.model.Chamado;
import br.com.conecta21.api.model.StatusChamado;
import br.com.conecta21.api.model.Usuario;
import br.com.conecta21.api.repository.AvaliacaoRepository;
import br.com.conecta21.api.repository.ChamadoRepository;
import br.com.conecta21.api.security.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Avaliação de atendimento (Backend B — fluxo de encerramento / NPS).
 *
 * <p>Regras: só o solicitante avalia, só quando o chamado está RESOLVIDO,
 * uma única avaliação por chamado. Tenant isolado via JWT.
 */
@Service
public class AvaliacaoService {

    @Autowired
    private AvaliacaoRepository avaliacaoRepository;

    @Autowired
    private ChamadoRepository chamadoRepository;

    @Autowired
    private TenantContext tenantContext;

    @Transactional
    public AvaliacaoRespostaDTO criar(Long chamadoId, AvaliacaoCriacaoDTO dto) {
        Long empresaId = tenantContext.getEmpresaIdAutenticada();
        Usuario avaliador = tenantContext.getUsuarioAutenticado();

        Chamado chamado = chamadoRepository.findByIdAndEmpresaId(chamadoId, empresaId)
                .orElseThrow(() -> new EntityNotFoundException("Chamado não encontrado."));

        if (!StatusChamado.RESOLVIDO.equals(chamado.getStatus())) {
            throw new IllegalArgumentException("Chamado ainda não foi resolvido. Avaliação liberada apenas após RESOLVIDO.");
        }

        if (!chamado.getSolicitante().getId().equals(avaliador.getId())) {
            throw new AccessDeniedException("Apenas o solicitante do chamado pode avaliar o atendimento.");
        }

        if (avaliacaoRepository.existsByChamadoId(chamadoId)) {
            throw new IllegalStateException("Este chamado já possui uma avaliação.");
        }

        Avaliacao avaliacao = new Avaliacao();
        avaliacao.setChamado(chamado);
        avaliacao.setNota(dto.nota());
        avaliacao.setComentario(normalizarComentario(dto.comentario()));

        return toResposta(avaliacaoRepository.save(avaliacao));
    }

    @Transactional(readOnly = true)
    public AvaliacaoRespostaDTO buscar(Long chamadoId) {
        Long empresaId = tenantContext.getEmpresaIdAutenticada();

        if (chamadoRepository.findByIdAndEmpresaId(chamadoId, empresaId).isEmpty()) {
            throw new EntityNotFoundException("Chamado não encontrado.");
        }

        Avaliacao avaliacao = avaliacaoRepository
                .findByChamadoIdAndChamadoEmpresaId(chamadoId, empresaId)
                .orElseThrow(() -> new EntityNotFoundException("Avaliação não encontrada."));

        return toResposta(avaliacao);
    }

    private String normalizarComentario(String comentario) {
        if (comentario == null) {
            return null;
        }
        String normalizado = comentario.trim();
        return normalizado.isEmpty() ? null : normalizado;
    }

    private AvaliacaoRespostaDTO toResposta(Avaliacao avaliacao) {
        return new AvaliacaoRespostaDTO(
                avaliacao.getId(),
                avaliacao.getChamado().getId(),
                avaliacao.getChamado().getSolicitante().getId(),
                avaliacao.getNota(),
                avaliacao.getComentario(),
                avaliacao.getDataCriacao());
    }
}
