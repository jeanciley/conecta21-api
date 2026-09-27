package br.com.conecta21.api.controller;

import br.com.conecta21.api.TokenService.TokenService;
import br.com.conecta21.api.dto.DadosLoginDTO;
import br.com.conecta21.api.dto.DadosTokenJWT;
import br.com.conecta21.api.model.Usuario;
import br.com.conecta21.api.dto.AtivacaoDTO;
import br.com.conecta21.api.dto.EmailDTO;
import br.com.conecta21.api.service.FluxoSenhaService;
import br.com.conecta21.api.security.LoginRateLimiterService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager manager;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private FluxoSenhaService fluxoSenhaService;

    @Autowired
    private LoginRateLimiterService rateLimiter;

    @PostMapping
    public ResponseEntity<?> efetuarLogin(@RequestBody @Valid DadosLoginDTO dados) {

        String email = dados.email().trim();
        if (rateLimiter.estaBloqueado(email)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Muitas tentativas de login falhas. Tente novamente em 1 minuto.");
        }

        try {
            var authenticationToken = new UsernamePasswordAuthenticationToken(email, dados.senha());
            var authentication = manager.authenticate(authenticationToken);
            rateLimiter.limparTentativas(email);
            var tokenJWT = tokenService.gerarToken((Usuario) authentication.getPrincipal());
            return ResponseEntity.ok(new DadosTokenJWT(tokenJWT));
        } catch (AuthenticationException exception) {
            rateLimiter.registrarFalha(email);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("E-mail ou senha invÃ¡lidos.");
        }
    }

    @PostMapping("/ativacao")
    public ResponseEntity<Void> ativar(@RequestBody @Valid AtivacaoDTO dto) { fluxoSenhaService.ativar(dto); return ResponseEntity.noContent().build(); }

    @PostMapping("/esqueci-senha")
    public ResponseEntity<Void> solicitarRedefinicao(@RequestBody @Valid EmailDTO dto) { fluxoSenhaService.solicitarRedefinicao(dto.email()); return ResponseEntity.accepted().build(); }

    @PostMapping("/redefinir-senha")
    public ResponseEntity<Void> redefinir(@RequestBody @Valid AtivacaoDTO dto) { fluxoSenhaService.redefinir(dto); return ResponseEntity.noContent().build(); }
}
