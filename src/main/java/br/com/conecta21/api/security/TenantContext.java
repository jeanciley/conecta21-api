package br.com.conecta21.api.security;

import br.com.conecta21.api.model.Usuario;
import br.com.conecta21.api.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Abstração simples para expor o tenant do usuário autenticado (Backend C).
 *
 * <p>O {@code empresa_id} viaja no JWT, mas o filtro do Backend Jean disponibiliza
 * no {@link SecurityContextHolder} a entidade {@link Usuario} como principal.
 * Este componente apenas lê esse principal — não altera JWT, filtro ou segurança.
 *
 * <p>O usuário é recarregado pelo id para que {@code getEmpresa()} funcione
 * mesmo com o principal fora de sessão JPA (associação LAZY).
 */
@Component
public class TenantContext {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public Usuario getUsuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Usuario principal)) {
            throw new AccessDeniedException("Usuário não autenticado.");
        }
        return usuarioRepository.findById(principal.getId())
                .orElseThrow(() -> new AccessDeniedException("Usuário não autenticado."));
    }

    @Transactional(readOnly = true)
    public Long getEmpresaIdAutenticada() {
        return getUsuarioAutenticado().getEmpresa().getId();
    }
}
