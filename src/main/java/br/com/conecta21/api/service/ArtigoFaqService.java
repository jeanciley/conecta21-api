package br.com.conecta21.api.service;

import br.com.conecta21.api.dto.ArtigoCriacaoDTO;
import br.com.conecta21.api.dto.ArtigoRespostaDTO;
import br.com.conecta21.api.model.ArtigoFaq;
import br.com.conecta21.api.model.PerfilUsuario;
import br.com.conecta21.api.model.Usuario;
import br.com.conecta21.api.repository.ArtigoFaqRepository;
import br.com.conecta21.api.repository.ArtigoFaqSpecs;
import br.com.conecta21.api.security.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ArtigoFaqService {

    @Autowired
    private ArtigoFaqRepository artigoRepository;

    @Autowired
    private TenantContext tenantContext;

    @Transactional
    public ArtigoRespostaDTO criar(ArtigoCriacaoDTO dto) {
        Usuario autor = tenantContext.getUsuarioAutenticado();
        Long empresaId = tenantContext.getEmpresaIdAutenticada();

        if (!empresaId.equals(autor.getEmpresa().getId())) {
            throw new AccessDeniedException("Divergência de tenant entre token e usuário.");
        }
        if (PerfilUsuario.USUARIO.equals(autor.getPerfil())) {
            throw new AccessDeniedException("Apenas técnicos e administradores podem publicar artigos.");
        }

        ArtigoFaq artigo = new ArtigoFaq();
        artigo.setTitulo(dto.titulo().trim());
        artigo.setConteudo(dto.conteudo().trim());
        artigo.setAutor(autor);
        artigo.setEmpresa(autor.getEmpresa());

        return toRespostaDTO(artigoRepository.save(artigo));
    }

    @Transactional(readOnly = true)
    public Page<ArtigoRespostaDTO> listar(String busca, Pageable pageable) {
        Long empresaId = tenantContext.getEmpresaIdAutenticada();

        if (busca != null && !busca.isBlank()) {
            Pageable paginaSemOrdenacaoExterna = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
            return artigoRepository
                    .pesquisarFullText(empresaId, busca.trim(), paginaSemOrdenacaoExterna)
                    .map(this::toRespostaDTO);
        }

        return artigoRepository
                .findAll(ArtigoFaqSpecs.doTenant(empresaId), pageable)
                .map(this::toRespostaDTO);
    }

    @Transactional(readOnly = true)
    public ArtigoRespostaDTO detalhar(Long id) {
        Long empresaId = tenantContext.getEmpresaIdAutenticada();
        ArtigoFaq artigo = artigoRepository.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> new EntityNotFoundException("Artigo não encontrado."));
        return toRespostaDTO(artigo);
    }

    private ArtigoRespostaDTO toRespostaDTO(ArtigoFaq artigo) {
        return new ArtigoRespostaDTO(
                artigo.getId(),
                artigo.getTitulo(),
                artigo.getConteudo(),
                artigo.getAutor().getId(),
                artigo.getAutor().getNome(),
                artigo.getDataCriacao());
    }
}
