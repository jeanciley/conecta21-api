package br.com.conecta21.api.service;

import br.com.conecta21.api.dto.ArtigoCriacaoDTO;
import br.com.conecta21.api.dto.ArtigoRespostaDTO;
import br.com.conecta21.api.model.ArtigoFaq;
import br.com.conecta21.api.model.PerfilUsuario;
import br.com.conecta21.api.model.Usuario;
import br.com.conecta21.api.repository.ArtigoFaqRepository;
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

    @Transactional
    public ArtigoRespostaDTO criar(ArtigoCriacaoDTO dto){
        Usuario autorLogado = tenantContext.getUsuarioAutenticado();

        if (PerfilUsuario.USUARIO.equals(autorLogado.getPerfil())){
            throw new IllegalArgumentException("Acesso negado. Apenas técnicos e administradores podem publicar artigos.");
        }

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


    private ArtigoRespostaDTO toRespostaDTO(ArtigoFaq artigo) {
        return new ArtigoRespostaDTO(
                artigo.getId(),
                artigo.getTitulo(),
                artigo.getConteudo(),
                artigo.getAutor().getId(),
                artigo.getAutor().getNome(),
                artigo.getDataCriacao()
        );
    }
}
