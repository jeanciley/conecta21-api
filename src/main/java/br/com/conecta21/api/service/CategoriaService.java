package br.com.conecta21.api.service;

import br.com.conecta21.api.dto.CategoriaCriacaoDTO;
import br.com.conecta21.api.dto.CategoriaRespostaDTO;
import br.com.conecta21.api.model.Categoria;
import br.com.conecta21.api.model.Usuario;
import br.com.conecta21.api.repository.CategoriaRepository;
import br.com.conecta21.api.security.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoriaService {

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private TenantContext tenantContext;

    @Transactional
    public CategoriaRespostaDTO criar(CategoriaCriacaoDTO dto) {
        Usuario usuarioLogado = tenantContext.getUsuarioAutenticado();

        Categoria categoria = new Categoria();
        categoria.setNome(dto.nome());
        categoria.setEmpresa(usuarioLogado.getEmpresa());

        Categoria salva = categoriaRepository.save(categoria);

        return new CategoriaRespostaDTO(salva.getId(), salva.getNome());
    }

    @Transactional(readOnly = true)
    public List<CategoriaRespostaDTO> listar() {
        Long empresaId = tenantContext.getEmpresaIdAutenticada();

        return categoriaRepository.findAllByEmpresaId(empresaId)
                .stream()
                .map(cat -> new CategoriaRespostaDTO(cat.getId(), cat.getNome()))
                .toList();
    }
}