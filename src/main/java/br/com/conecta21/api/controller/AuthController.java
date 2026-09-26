package br.com.conecta21.api.controller;

import br.com.conecta21.api.TokenService.TokenService;
import br.com.conecta21.api.dto.DadosLoginDTO;
import br.com.conecta21.api.dto.DadosTokenJWT;
import br.com.conecta21.api.model.Usuario;
import br.com.conecta21.api.dto.AtivacaoDTO;
import br.com.conecta21.api.dto.EmailDTO;
import br.com.conecta21.api.service.FluxoSenhaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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

    @PostMapping
    public ResponseEntity<?> efetuarLogin(@RequestBody DadosLoginDTO dados) {

        // 1. Intercepta e consome uma ficha do balde ligado ao e-mail informado
        io.github.bucket4j.Bucket bucket = rateLimiter.resolverBucket(dados.email());

        if (!bucket.tryConsume(1)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Muitas tentativas de login falhas. Tente novamente em 1 minuto.");
        }

        var authenticationToken = new UsernamePasswordAuthenticationToken(dados.email(), dados.senha());
        var authentication = manager.authenticate(authenticationToken);
        var tokenJWT = tokenService.gerarToken((Usuario) authentication.getPrincipal());

        return ResponseEntity.ok(new DadosTokenJWT(tokenJWT));
    }

    @PostMapping("/ativacao")
    public ResponseEntity<Void> ativar(@RequestBody @Valid AtivacaoDTO dto) { fluxoSenhaService.ativar(dto); return ResponseEntity.noContent().build(); }

    @PostMapping("/esqueci-senha")
    public ResponseEntity<Void> solicitarRedefinicao(@RequestBody @Valid EmailDTO dto) { fluxoSenhaService.solicitarRedefinicao(dto.email()); return ResponseEntity.accepted().build(); }

    @PostMapping("/redefinir-senha")
    public ResponseEntity<Void> redefinir(@RequestBody @Valid AtivacaoDTO dto) { fluxoSenhaService.redefinir(dto); return ResponseEntity.noContent().build(); }
}
