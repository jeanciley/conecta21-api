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
import br.com.conecta21.api.model.Categoria;
import br.com.conecta21.api.repository.CategoriaRepository;
import java.util.List;

@Service
public class ArtigoFaqService {

    @Autowired
    private ArtigoFaqRepository artigoRepository;

    @Autowired
    private TenantContext tenantContext;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Transactional
    public ArtigoRespostaDTO criar(ArtigoCriacaoDTO dto){
        Usuario autorLogado = tenantContext.getUsuarioAutenticado();
        Long empresaId = autorLogado.getEmpresa().getId();

        if (PerfilUsuario.USUARIO.equals(autorLogado.getPerfil())){
            throw new IllegalArgumentException("Acesso negado. Apenas técnicos e administradores podem publicar artigos.");
        }

        if (artigoRepository.existsByTituloIgnoreCaseAndEmpresaId(dto.titulo(), empresaId)) {
            throw new IllegalArgumentException("Já existe um artigo com este título na sua base de conhecimento.");
        }

        Categoria categoria = categoriaRepository.findByIdAndEmpresaId(dto.categoriaId(), empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada."));

        ArtigoFaq artigo = new ArtigoFaq();
        artigo.setTitulo(dto.titulo());
        artigo.setConteudo(dto.conteudo());
        artigo.setAutor(autorLogado);
        artigo.setEmpresa(autorLogado.getEmpresa());

        ArtigoFaq salvo = artigoRepository.save(artigo);

        return toRespostaDTO(salvo);
    }

    @Transactional(readOnly = true)
    public List<ArtigoRespostaDTO> listar() {
        Long empresaId = tenantContext.getEmpresaIdAutenticada();

        return artigoRepository.findAllByEmpresaId(empresaId)
                .stream()
                .map(this::toRespostaDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public ArtigoRespostaDTO detalhar(Long id) {
        Long empresaId = tenantContext.getEmpresaIdAutenticada();

        ArtigoFaq artigo = artigoRepository.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> new EntityNotFoundException("Artigo não encontrado ou não pertence a esta empresa."));

        return toRespostaDTO(artigo);
    }

    @Transactional(readOnly = true)
    public List<ArtigoRespostaDTO> buscarPorTermo(String termo) {
        Long empresaId = tenantContext.getEmpresaIdAutenticada();

        return artigoRepository.buscarPorTermo(empresaId, termo)
                .stream()
                .map(this::toRespostaDTO)
                .toList();
    }

    private ArtigoRespostaDTO toRespostaDTO(ArtigoFaq artigo) {
        return new ArtigoRespostaDTO(
                artigo.getId(),
                artigo.getTitulo(),
                artigo.getConteudo(),
                artigo.getAutor().getId(),
                artigo.getAutor().getNome(),
                artigo.getCategoria().getNome(),
                artigo.getDataCriacao()
        );
    }
}
