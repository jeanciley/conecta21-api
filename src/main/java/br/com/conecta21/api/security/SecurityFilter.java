package br.com.conecta21.api.security;

import br.com.conecta21.api.TokenService.TokenService;
import br.com.conecta21.api.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class SecurityFilter extends OncePerRequestFilter {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        var tokenJWT = recuperarToken(request);

        if (tokenJWT != null) {
            try {
                var subject = tokenService.getSubject(tokenJWT);
                if (subject == null || subject.isBlank()) {
                    filterChain.doFilter(request, response);
                    return;
                }
                // O id torna o token resiliente à alteração de e-mail do próprio perfil.
                // Tokens antigos continuam compatíveis por meio do subject.
                var usuarioId = tokenService.getUsuarioId(tokenJWT);
                var empresaId = tokenService.getEmpresaId(tokenJWT);
                var usuario = usuarioId != null
                        ? usuarioRepository.findById(usuarioId).orElseThrow(() -> new RuntimeException("Usuário não encontrado"))
                        : usuarioRepository.findByEmail(subject).orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

                // Força a autenticação no contexto do Spring Security
                var authentication = new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities());
                // Disponibiliza o tenant para o TenantContext (via details + request attribute).
                authentication.setDetails(java.util.Map.of("empresaId", empresaId));
                SecurityContextHolder.getContext().setAuthentication(authentication);
                request.setAttribute("empresaId", empresaId);
            } catch (RuntimeException ex) {
                SecurityContextHolder.clearContext();
            }
        }

        // Libera a requisição para seguir o fluxo até o Controller
        filterChain.doFilter(request, response);
    }

    private String recuperarToken(HttpServletRequest request) {
        var authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null) {
            return authorizationHeader.replace("Bearer ", "");
        }
        return null;
    }
}
