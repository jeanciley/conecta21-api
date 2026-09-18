package br.com.conecta21.api.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LoginRateLimiterService {

    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    public Bucket resolverBucket(String email) {
        return cache.computeIfAbsent(email, this::criarNovoBucket);
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
