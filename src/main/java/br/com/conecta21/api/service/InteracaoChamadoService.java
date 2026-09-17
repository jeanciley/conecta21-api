package br.com.conecta21.api.service;

import br.com.conecta21.api.dto.InteracaoCriacaoDTO;
import br.com.conecta21.api.dto.InteracaoRespostaDTO;
import br.com.conecta21.api.model.Chamado;
import br.com.conecta21.api.model.InteracaoChamado;
import br.com.conecta21.api.model.Usuario;
import br.com.conecta21.api.repository.ChamadoRepository;
import br.com.conecta21.api.repository.InteracaoChamadoRepository;
import br.com.conecta21.api.repository.UsuarioRepository;
import br.com.conecta21.api.security.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Histórico e comentários do chamado (Backend C — Evolução Operacional).
 *
 * <p>Trava SaaS: o {@code empresa_id} vem exclusivamente do JWT
 * (via {@link TenantContext#getEmpresaIdAutenticada()}). O chamado é sempre
 * resolvido por {@code findByIdAndEmpresaId}; chamado de outro tenant resulta
 * em 404. O autor é o usuário autenticado e precisa pertencer ao mesmo tenant
 * do chamado.
 */
@Service
public class InteracaoChamadoService {

    @Autowired
    private InteracaoChamadoRepository interacaoRepository;

    @Autowired
    private ChamadoRepository chamadoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TenantContext tenantContext;

    @Transactional
    public InteracaoRespostaDTO comentar(Long chamadoId, InteracaoCriacaoDTO dto) {
        Long empresaId = tenantContext.getEmpresaIdAutenticada();
        Usuario autor = tenantContext.getUsuarioAutenticado();

        Chamado chamado = chamadoRepository.findByIdAndEmpresaId(chamadoId, empresaId)
                .orElseThrow(() -> new EntityNotFoundException("Chamado não encontrado."));

        if (!autor.getEmpresa().getId().equals(chamado.getEmpresa().getId())) {
            throw new AccessDeniedException("Divergência de tenant entre token e usuário.");
        }

        InteracaoChamado interacao = new InteracaoChamado();
        interacao.setChamado(chamado);
        interacao.setAutor(usuarioRepository.getReferenceById(autor.getId()));
        interacao.setMensagem(dto.mensagem().trim());

        return toResposta(interacaoRepository.save(interacao));
    }

    @Transactional(readOnly = true)
    public Page<InteracaoRespostaDTO> listar(Long chamadoId, Pageable pageable) {
        Long empresaId = tenantContext.getEmpresaIdAutenticada();

        // Valida o tenant antes de expor qualquer mensagem (404 cross-tenant).
        if (chamadoRepository.findByIdAndEmpresaId(chamadoId, empresaId).isEmpty()) {
            throw new EntityNotFoundException("Chamado não encontrado.");
        }

        return interacaoRepository
                .findAllByChamadoIdAndChamadoEmpresaIdOrderByDataCriacaoAsc(chamadoId, empresaId, pageable)
                .map(this::toResposta);
    }

    private InteracaoRespostaDTO toResposta(InteracaoChamado interacao) {
        return new InteracaoRespostaDTO(
                interacao.getId(),
                interacao.getChamado().getId(),
                interacao.getAutor().getId(),
                interacao.getAutor().getNome(),
                interacao.getMensagem(),
                interacao.getDataCriacao());
    }
}
