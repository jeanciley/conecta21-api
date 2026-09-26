package br.com.conecta21.api.service;

import br.com.conecta21.api.dto.InteracaoCriacaoDTO;
import br.com.conecta21.api.dto.InteracaoRespostaDTO;
import br.com.conecta21.api.dto.AnexoArquivoDTO;
import br.com.conecta21.api.dto.AnexoInteracaoRespostaDTO;
import br.com.conecta21.api.model.AnexoInteracao;
import br.com.conecta21.api.model.Chamado;
import br.com.conecta21.api.model.InteracaoChamado;
import br.com.conecta21.api.model.Usuario;
import br.com.conecta21.api.repository.ChamadoRepository;
import br.com.conecta21.api.repository.InteracaoChamadoRepository;
import br.com.conecta21.api.repository.AnexoInteracaoRepository;
import br.com.conecta21.api.repository.UsuarioRepository;
import br.com.conecta21.api.security.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

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
    private AnexoInteracaoRepository anexoInteracaoRepository;

    @Autowired
    private ChamadoRepository chamadoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TenantContext tenantContext;

    @Transactional
    public InteracaoRespostaDTO comentar(Long chamadoId, InteracaoCriacaoDTO dto) {
        return comentar(chamadoId, dto, List.of());
    }

    @Transactional
    public InteracaoRespostaDTO comentar(Long chamadoId, InteracaoCriacaoDTO dto, List<MultipartFile> arquivos) {
        if (dto.mensagem() == null || dto.mensagem().isBlank() || dto.mensagem().length() > 4000) {
            throw new IllegalArgumentException("A mensagem deve ter entre 1 e 4000 caracteres.");
        }
        if (dto.tipo() != null && dto.tipo().length() > 40) {
            throw new IllegalArgumentException("O tipo da interação deve ter no máximo 40 caracteres.");
        }
        Long empresaId = tenantContext.getEmpresaIdAutenticada();
        Usuario autor = tenantContext.getUsuarioAutenticado();

        Chamado chamado = chamadoRepository.findByIdAndEmpresaId(chamadoId, empresaId)
                .orElseThrow(() -> new EntityNotFoundException("Chamado não encontrado."));

        if (!autor.getEmpresa().getId().equals(chamado.getEmpresa().getId())) {
            throw new AccessDeniedException("Divergência de tenant entre token e usuário.");
        }

        InteracaoChamado interacao = new InteracaoChamado();
        if ((autor.getPerfil() == br.com.conecta21.api.model.PerfilUsuario.TECNICO
                || autor.getPerfil() == br.com.conecta21.api.model.PerfilUsuario.ADMIN)
                && chamado.getDataPrimeiraResposta() == null) {
            chamado.setDataPrimeiraResposta(java.time.LocalDateTime.now());
        }
        interacao.setChamado(chamado);
        interacao.setAutor(usuarioRepository.getReferenceById(autor.getId()));
        interacao.setMensagem(dto.mensagem().trim());
        interacao.setTipo(dto.tipo() == null || dto.tipo().isBlank() ? "Comentário" : dto.tipo().trim());

        if (arquivos != null && arquivos.size() > 5) {
            throw new IllegalArgumentException("É permitido anexar no máximo 5 imagens por interação.");
        }
        if (arquivos != null) {
            for (MultipartFile arquivo : arquivos) {
                if (arquivo == null || arquivo.isEmpty()) continue;
                String contentType = arquivo.getContentType();
                if (arquivo.getSize() > 5L * 1024 * 1024) {
                    throw new IllegalArgumentException("Cada imagem deve ter no máximo 5 MB.");
                }
                if (contentType == null || !List.of("image/jpeg", "image/png", "image/gif", "image/webp")
                        .contains(contentType.toLowerCase(Locale.ROOT))) {
                    throw new IllegalArgumentException("Formato de imagem não suportado.");
                }
                try {
                    AnexoInteracao anexo = new AnexoInteracao();
                    anexo.setInteracao(interacao);
                    String nome = arquivo.getOriginalFilename();
                    nome = nome == null ? "imagem" : nome.replace('\\', '/');
                    int separador = nome.lastIndexOf('/');
                    if (separador >= 0) nome = nome.substring(separador + 1);
                    nome = nome.replaceAll("[\\p{Cntrl}]", "_");
                    anexo.setNomeArquivo(nome.length() > 255 ? nome.substring(0, 255) : nome);
                    anexo.setContentType(contentType.toLowerCase(Locale.ROOT));
                    anexo.setTamanho(arquivo.getSize());
                    anexo.setConteudo(arquivo.getBytes());
                    interacao.getAnexos().add(anexo);
                } catch (IOException ex) {
                    throw new IllegalArgumentException("Não foi possível ler uma das imagens anexadas.", ex);
                }
            }
        }

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

    @Transactional(readOnly = true)
    public InteracaoRespostaDTO detalhar(Long chamadoId, Long interacaoId) {
        Long empresaId = tenantContext.getEmpresaIdAutenticada();
        InteracaoChamado interacao = interacaoRepository
                .findByIdAndChamadoIdAndChamadoEmpresaId(interacaoId, chamadoId, empresaId)
                .orElseThrow(() -> new EntityNotFoundException("Interação não encontrada."));
        return toResposta(interacao);
    }

    @Transactional(readOnly = true)
    public AnexoArquivoDTO baixarAnexo(Long chamadoId, Long interacaoId, Long anexoId) {
        Long empresaId = tenantContext.getEmpresaIdAutenticada();
        AnexoInteracao anexo = anexoInteracaoRepository
                .findByIdAndInteracaoIdAndInteracaoChamadoIdAndInteracaoChamadoEmpresaId(
                        anexoId, interacaoId, chamadoId, empresaId)
                .orElseThrow(() -> new EntityNotFoundException("Anexo não encontrado."));
        return new AnexoArquivoDTO(anexo.getNomeArquivo(), anexo.getContentType(), anexo.getConteudo());
    }

    private InteracaoRespostaDTO toResposta(InteracaoChamado interacao) {
        return new InteracaoRespostaDTO(
                interacao.getId(),
                interacao.getChamado().getId(),
                interacao.getAutor().getId(),
                interacao.getAutor().getNome(),
                interacao.getTipo(),
                interacao.getMensagem(),
                interacao.getDataCriacao(),
                interacao.getAnexos().stream()
                        .map(anexo -> new AnexoInteracaoRespostaDTO(
                                anexo.getId(), anexo.getNomeArquivo(), anexo.getContentType(), anexo.getTamanho()))
                        .toList());
    }
}
