package br.com.conecta21.api.service;

import br.com.conecta21.api.dto.PerfilCustomizadoDTO;
import br.com.conecta21.api.model.PerfilCustomizado;
import br.com.conecta21.api.model.PerfilUsuario;
import br.com.conecta21.api.repository.PerfilCustomizadoRepository;
import br.com.conecta21.api.repository.UsuarioRepository;
import br.com.conecta21.api.security.TenantContext;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PerfilCustomizadoService {
    private static final Set<String> PERMISSOES_VALIDAS = Set.of("CHAMADOS_INTERNOS", "GERENCIAR_CHAMADOS", "GERENCIAR_CATEGORIAS", "GERENCIAR_PRIORIDADES", "GERENCIAR_USUARIOS", "GERENCIAR_FAQ", "GERAR_RELATORIOS");
    private final PerfilCustomizadoRepository perfis;
    private final UsuarioRepository usuarios;
    private final TenantContext tenant;
    public PerfilCustomizadoService(PerfilCustomizadoRepository perfis, UsuarioRepository usuarios, TenantContext tenant) { this.perfis=perfis; this.usuarios=usuarios; this.tenant=tenant; }

    @Transactional(readOnly=true)
    public java.util.List<PerfilCustomizadoDTO> listar() {
        if (!tenant.getUsuarioAutenticado().temPermissao("GERENCIAR_USUARIOS")) throw new AccessDeniedException("Sem permissÃ£o para consultar perfis.");
        return perfis.findAllByEmpresaIdOrderByNome(tenant.getEmpresaIdAutenticada()).stream().map(this::dto).toList();
    }

    @Transactional
    public PerfilCustomizadoDTO salvar(Long id, PerfilCustomizadoDTO dto) {
        exigirAdmin();
        PerfilCustomizado perfil = id == null ? new PerfilCustomizado() : perfis.findByIdAndEmpresaId(id, tenant.getEmpresaIdAutenticada())
                .orElseThrow(() -> new EntityNotFoundException("Perfil não encontrado."));
        Set<String> permissoes = dto.permissoes() == null ? Set.of() : dto.permissoes().stream().map(String::trim).map(String::toUpperCase).collect(Collectors.toSet());
        if (!PERMISSOES_VALIDAS.containsAll(permissoes)) throw new IllegalArgumentException("O perfil contém permissões desconhecidas.");
        perfil.setEmpresa(tenant.getUsuarioAutenticado().getEmpresa()); perfil.setNome(dto.nome().trim());
        perfil.setDescricao(dto.descricao() == null ? null : dto.descricao().trim());
        perfil.setPermissoes(String.join(",", permissoes)); perfil.setAtivo(dto.ativo());
        return dto(perfis.save(perfil));
    }

    @Transactional
    public void desativar(Long id) {
        exigirAdmin();
        PerfilCustomizado perfil = perfis.findByIdAndEmpresaId(id, tenant.getEmpresaIdAutenticada()).orElseThrow(() -> new EntityNotFoundException("Perfil não encontrado."));
        if (usuarios.countByPerfilCustomizadoId(id) > 0) throw new IllegalArgumentException("Este perfil está atribuído a usuários. Reatribua-os antes de desativá-lo.");
        perfil.setAtivo(false);
    }

    private void exigirAdmin() { if (tenant.getUsuarioAutenticado().getPerfil() != PerfilUsuario.ADMIN) throw new AccessDeniedException("Somente administradores podem gerenciar perfis."); }
    private PerfilCustomizadoDTO dto(PerfilCustomizado perfil) {
        Set<String> permissoes = java.util.Arrays.stream(perfil.getPermissoes().split(",")).filter(s -> !s.isBlank()).collect(Collectors.toSet());
        return new PerfilCustomizadoDTO(perfil.getId(), perfil.getNome(), perfil.getDescricao(), permissoes, perfil.isAtivo());
    }
}
