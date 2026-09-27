package br.com.conecta21.api.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginRateLimiterService {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    public boolean estaBloqueado(String email) {
        return resolverBucket(email).getAvailableTokens() == 0;
    }

    public void registrarFalha(String email) {
        resolverBucket(email).tryConsume(1);
    }

    public void limparTentativas(String email) {
        cache.remove(normalizar(email));
    }

    private Bucket resolverBucket(String email) {
        String chave = normalizar(email);
        return cache.computeIfAbsent(chave, this::criarNovoBucket);
    }

    private String normalizar(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private Bucket criarNovoBucket(String email) {
        // Capacidade máxima de 3 tentativas.
        // O balde se enche novamente com 1 ficha a cada 1 minuto.
        Bandwidth limite = Bandwidth.builder()
                .capacity(3)
                .refillIntervally(1, Duration.ofMinutes(1))
                .build();

        return Bucket.builder()
                .addLimit(limite)
                .build();
    }
}
