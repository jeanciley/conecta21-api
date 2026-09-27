package br.com.conecta21.api.service;

import br.com.conecta21.api.dto.ArtigoCriacaoDTO;
import br.com.conecta21.api.dto.ArtigoRespostaDTO;
import br.com.conecta21.api.model.ArtigoFaq;
import br.com.conecta21.api.model.Categoria;
import br.com.conecta21.api.model.PerfilUsuario;
import br.com.conecta21.api.model.Usuario;
import br.com.conecta21.api.repository.ArtigoFaqRepository;
import br.com.conecta21.api.repository.CategoriaRepository;
import br.com.conecta21.api.security.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

        if (!autorLogado.temPermissao("GERENCIAR_FAQ")) throw new org.springframework.security.access.AccessDeniedException("Sem permissão para publicar artigos.");

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
        artigo.setCategoria(categoria);

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

    @Transactional
    public ArtigoRespostaDTO atualizar(Long id, ArtigoCriacaoDTO dto) {
        Usuario ator = tenantContext.getUsuarioAutenticado();
        if (!ator.temPermissao("GERENCIAR_FAQ")) throw new org.springframework.security.access.AccessDeniedException("Sem permissão para editar artigos.");
        Long empresaId = ator.getEmpresa().getId();
        ArtigoFaq artigo = artigoRepository.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> new EntityNotFoundException("Artigo não encontrado."));
        Categoria categoria = categoriaRepository.findByIdAndEmpresaId(dto.categoriaId(), empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Categoria não encontrada."));
        artigo.setTitulo(dto.titulo().trim()); artigo.setConteudo(dto.conteudo().trim()); artigo.setCategoria(categoria);
        return toRespostaDTO(artigoRepository.save(artigo));
    }

    @Transactional
    public void excluir(Long id) {
        Usuario ator = tenantContext.getUsuarioAutenticado();
        if (!ator.temPermissao("GERENCIAR_FAQ")) throw new org.springframework.security.access.AccessDeniedException("Sem permissão para excluir artigos.");
        ArtigoFaq artigo = artigoRepository.findByIdAndEmpresaId(id, ator.getEmpresa().getId())
                .orElseThrow(() -> new EntityNotFoundException("Artigo não encontrado."));
        artigoRepository.delete(artigo);
    }

    private ArtigoRespostaDTO toRespostaDTO(ArtigoFaq artigo) {
        return new ArtigoRespostaDTO(
                artigo.getId(),
                artigo.getTitulo(),
                artigo.getConteudo(),
                artigo.getAutor().getId(),
                artigo.getAutor().getNome(),
                artigo.getCategoria().getId(),
                artigo.getCategoria().getNome(),
                artigo.getDataCriacao()
        );
    }
}
