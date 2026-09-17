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
 * Expõe o tenant do usuário autenticado (Backend C — validação estrita).
 *
 * <p>O {@code empresa_id} é extraído do claim JWT disponibilizado pelo
 * {@link SecurityFilter} (request attribute {@code empresaId} + details da
 * autenticação) e validado contra {@code Usuario.empresa.id} do banco.
 * Divergência resulta em {@link AccessDeniedException} (403).
 *
 * <p>Sem JWT presente (ex.: testes com {@code @WithMockUser}), faz fallback
 * para o {@code empresa.id} do usuário recarregado.
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
        Long empresaIdJwt = extrairEmpresaIdJwt();
        Usuario usuario = getUsuarioAutenticado();
        Long empresaIdBanco = usuario.getEmpresa().getId();
        if (empresaIdJwt != null && !empresaIdJwt.equals(empresaIdBanco)) {
            throw new AccessDeniedException("Divergência de tenant entre token e usuário.");
        }
        return empresaIdJwt != null ? empresaIdJwt : empresaIdBanco;
    }

    /**
     * Lê o empresa_id disponibilizado pelo {@link SecurityFilter}.
     * Ordem: request attribute {@code empresaId} → details da autenticação.
     * Retorna {@code null} quando não há JWT (fallback para o banco).
     */
    @SuppressWarnings("unchecked")
    private Long extrairEmpresaIdJwt() {
        // 1. Request attribute (via RequestContextHolder, sem injetar HttpServletRequest).
        try {
            var attrs = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                Object attr = attrs.getAttribute("empresaId",
                        org.springframework.web.context.request.RequestAttributes.SCOPE_REQUEST);
                if (attr instanceof Long l) {
                    return l;
                }
                if (attr instanceof Number n) {
                    return n.longValue();
                }
            }
        } catch (Exception ignored) {
            // Sem contexto de request (ex.: testes unitários) → tenta via SecurityContext.
        }
        // 2. Details da autenticação (Map.of("empresaId", id) definido no filtro).
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getDetails() instanceof java.util.Map<?, ?> details) {
                Object valor = details.get("empresaId");
                if (valor instanceof Long l) {
                    return l;
                }
                if (valor instanceof Number n) {
                    return n.longValue();
                }
            }
        } catch (Exception ignored) {
            // Ignora e deixa o fallback para o banco decidir.
        }
        return null;
    }
}
