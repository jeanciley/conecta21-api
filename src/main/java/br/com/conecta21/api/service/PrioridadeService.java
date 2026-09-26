package br.com.conecta21.api.service;

import br.com.conecta21.api.dto.PrioridadeDTO;
import br.com.conecta21.api.model.PerfilUsuario;
import br.com.conecta21.api.model.Prioridade;
import br.com.conecta21.api.repository.PrioridadeRepository;
import br.com.conecta21.api.repository.CategoriaRepository;
import br.com.conecta21.api.security.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class PrioridadeService {
    private final PrioridadeRepository repository;
    private final CategoriaRepository categorias;
    private final TenantContext tenant;
    public PrioridadeService(PrioridadeRepository repository, CategoriaRepository categorias, TenantContext tenant) { this.repository = repository; this.categorias = categorias; this.tenant = tenant; }

    @Transactional(readOnly = true)
    public List<PrioridadeDTO> listar(boolean somenteAtivas) {
        exigirAdmin();
        return repository.findAllByEmpresaIdOrderByNome(tenant.getEmpresaIdAutenticada()).stream()
                .filter(p -> !somenteAtivas || p.isAtiva()).map(this::dto).toList();
    }

    @Transactional
    public PrioridadeDTO salvar(Long id, PrioridadeDTO dto) {
        exigirAdmin();
        Long empresaId = tenant.getEmpresaIdAutenticada();
        Prioridade p = id == null ? new Prioridade() : repository.findByIdAndEmpresaId(id, empresaId)
                .orElseThrow(() -> new EntityNotFoundException("Prioridade não encontrada."));
        if (id != null && !dto.ativa() && p.isAtiva() && categorias.countByPrioridadeId(id) > 0)
            throw new IllegalArgumentException("Reatribua as categorias antes de desativar esta prioridade.");
        p.setEmpresa(tenant.getUsuarioAutenticado().getEmpresa());
        p.setNome(dto.nome().trim()); p.setSlaRespostaMinutos(dto.slaRespostaMinutos());
        p.setSlaResolucaoMinutos(dto.slaResolucaoMinutos()); p.setAtiva(dto.ativa());
        return dto(repository.save(p));
    }

    private void exigirAdmin() { if (tenant.getUsuarioAutenticado().getPerfil() != PerfilUsuario.ADMIN) throw new AccessDeniedException("Apenas administradores podem gerenciar prioridades."); }
    private PrioridadeDTO dto(Prioridade p) { return new PrioridadeDTO(p.getId(), p.getNome(), p.getSlaRespostaMinutos(), p.getSlaResolucaoMinutos(), p.isAtiva()); }
}
