package br.com.conecta21.api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter @Setter @Entity @Table(name = "tokens_usuario", indexes = @Index(name = "idx_token_usuario_hash", columnList = "token_hash", unique = true))
public class TokenUsuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "usuario_id", nullable = false) private Usuario usuario;
    @Column(name = "token_hash", nullable = false, unique = true, length = 64) private String tokenHash;
    @Column(nullable = false, length = 20) private String finalidade;
    @Column(name = "expira_em", nullable = false) private LocalDateTime expiraEm;
    @Column(name = "usado_em") private LocalDateTime usadoEm;
}
