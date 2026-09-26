package br.com.conecta21.api.service;

import br.com.conecta21.api.dto.CategoriaCriacaoDTO;
import br.com.conecta21.api.dto.CategoriaRespostaDTO;
import br.com.conecta21.api.model.Categoria;
import br.com.conecta21.api.model.Usuario;
import br.com.conecta21.api.model.PerfilUsuario;
import br.com.conecta21.api.repository.CategoriaRepository;
import br.com.conecta21.api.repository.PrioridadeRepository;
import org.springframework.security.access.AccessDeniedException;
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

    @Autowired
    private PrioridadeRepository prioridadeRepository;

    @Transactional
    public CategoriaRespostaDTO criar(CategoriaCriacaoDTO dto) {
        Usuario usuarioLogado = tenantContext.getUsuarioAutenticado();
        if (usuarioLogado.getPerfil() != PerfilUsuario.ADMIN) throw new AccessDeniedException("Apenas administradores podem gerenciar categorias.");

        Categoria categoria = new Categoria();
        categoria.setNome(dto.nome());
        categoria.setEmpresa(usuarioLogado.getEmpresa());
        categoria.setPrioridade(prioridadeRepository.findByIdAndEmpresaId(dto.prioridadeId(), usuarioLogado.getEmpresa().getId())
                .filter(p -> p.isAtiva()).orElseThrow(() -> new IllegalArgumentException("Prioridade ativa não encontrada.")));

        Categoria salva = categoriaRepository.save(categoria);

        return new CategoriaRespostaDTO(salva.getId(), salva.getNome());
    }

    @Transactional(readOnly = true)
    public List<CategoriaRespostaDTO> listar() {
        Long empresaId = tenantContext.getEmpresaIdAutenticada();

        return categoriaRepository.findAllByEmpresaId(empresaId).stream().filter(Categoria::isAtiva)
                .map(cat -> new CategoriaRespostaDTO(cat.getId(), cat.getNome()))
                .toList();
    }
}
