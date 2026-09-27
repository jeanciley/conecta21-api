package br.com.conecta21.api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "logs_auditoria")
public class LogAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "empresa_id", nullable = false)
    private Long empresaId;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "nome_usuario", nullable = false)
    private String nomeUsuario;

    @Column(nullable = false)
    private String acao; // Ex: "ALTERACAO_STATUS", "CRIACAO"

    @Column(nullable = false)
    private String entidade; // Ex: "Chamado", "ArtigoFaq"

    @Column(name = "entidade_id", nullable = false)
    private Long entidadeId;

    @Column(columnDefinition = "TEXT")
    private String detalhes; // Ex: "Mudou de ABERTO para EM_ANDAMENTO"

    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @PrePersist
    protected void onCreate() {
        if (dataCriacao == null) {
            dataCriacao = LocalDateTime.now();
        }
    }
}
