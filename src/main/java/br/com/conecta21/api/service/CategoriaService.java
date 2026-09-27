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
        if (!usuarioLogado.temPermissao("GERENCIAR_CATEGORIAS")) throw new AccessDeniedException("Sem permissão para gerenciar categorias.");

        Categoria categoria = new Categoria();
        categoria.setNome(dto.nome());
        categoria.setEmpresa(usuarioLogado.getEmpresa());
        categoria.setPrioridade(prioridadeRepository.findByIdAndEmpresaId(dto.prioridadeId(), usuarioLogado.getEmpresa().getId())
                .filter(p -> p.isAtiva()).orElseThrow(() -> new IllegalArgumentException("Prioridade ativa não encontrada.")));

        Categoria salva = categoriaRepository.save(categoria);

        return toResposta(salva, true);
    }

    @Transactional
    public CategoriaRespostaDTO atualizar(Long id, CategoriaCriacaoDTO dto) {
        Usuario usuario = tenantContext.getUsuarioAutenticado();
        if (!usuario.temPermissao("GERENCIAR_CATEGORIAS")) throw new AccessDeniedException("Sem permissão para gerenciar categorias.");
        Categoria categoria = categoriaRepository.findByIdAndEmpresaId(id, usuario.getEmpresa().getId())
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Categoria não encontrada."));
        categoria.setNome(dto.nome().trim());
        categoria.setPrioridade(prioridadeRepository.findByIdAndEmpresaId(dto.prioridadeId(), usuario.getEmpresa().getId())
                .filter(p -> p.isAtiva()).orElseThrow(() -> new IllegalArgumentException("Prioridade ativa não encontrada.")));
        return toResposta(categoriaRepository.save(categoria), true);
    }

    @Transactional(readOnly = true)
    public List<CategoriaRespostaDTO> listar() {
        Usuario ator = tenantContext.getUsuarioAutenticado();
        Long empresaId = ator.getEmpresa().getId();
        boolean podeGerenciar = ator.temPermissao("GERENCIAR_CATEGORIAS");

        return categoriaRepository.findAllByEmpresaId(empresaId).stream().filter(Categoria::isAtiva)
                .map(cat -> toResposta(cat, podeGerenciar))
                .toList();
    }

    private CategoriaRespostaDTO toResposta(Categoria categoria, boolean incluirPrioridade) {
        return new CategoriaRespostaDTO(categoria.getId(), categoria.getNome(),
                incluirPrioridade && categoria.getPrioridade() != null ? categoria.getPrioridade().getId() : null,
                incluirPrioridade && categoria.getPrioridade() != null ? categoria.getPrioridade().getNome() : null);
    }
}
