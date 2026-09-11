package br.com.conecta21.api.TokenService;

import br.com.conecta21.api.model.Usuario;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class TokenService {

    @Value("${api.security.token.secret}")
    private String secret;

    public String gerarToken(Usuario usuario) {
        try {
            Algorithm algoritmo = Algorithm.HMAC256(secret);
            return JWT.create()
                    .withIssuer("conecta21-api")
                    .withSubject(usuario.getUsername())
                    .withClaim("empresa_id", usuario.getEmpresa().getId())
                    .withClaim("perfil", usuario.getPerfil())
                    .withExpiresAt(dataExpiracao())
                    .sign(algoritmo);
        } catch (JWTCreationException exception) {
            throw new RuntimeException("Erro ao gerar token JWT", exception);
        }
    }

    public String getSubject(String tokenJWT) {
        try {
            Algorithm algoritmo = Algorithm.HMAC256(secret);
            return JWT.require(algoritmo)
                    .withIssuer("conecta21-api")
                    .build()
                    .verify(tokenJWT)
                    .getSubject();
        } catch (JWTVerificationException exception) {
            return "";
        }
    }

    /**
     * Extrai o {@code empresa_id} do claim JWT (Backend C — validação estrita de tenant).
     *
     * @throws RuntimeException se o token for inválido ou o claim estiver ausente.
     */
    public Long getEmpresaId(String tokenJWT) {
        try {
            Algorithm algoritmo = Algorithm.HMAC256(secret);
            Long empresaId = JWT.require(algoritmo)
                    .withIssuer("conecta21-api")
                    .build()
                    .verify(tokenJWT)
                    .getClaim("empresa_id").asLong();
            if (empresaId == null) {
                throw new RuntimeException("Token JWT sem claim empresa_id.");
            }
            return empresaId;
        } catch (JWTVerificationException exception) {
            throw new RuntimeException("Token JWT inválido ou expirado.", exception);
        }
    }

    private Instant dataExpiracao() {
        // Define que o token expira em 2 horas
        return LocalDateTime.now().plusHours(2).toInstant(ZoneOffset.of("-03:00"));
    }
}
