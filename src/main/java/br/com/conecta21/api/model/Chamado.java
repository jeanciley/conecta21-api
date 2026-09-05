package br.com.conecta21.api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "chamados")
public class Chamado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitante_id", nullable = false)
    private Usuario solicitante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tecnico_id")
    private Usuario tecnico;

    @Setter
    @Column(nullable = false, length = 150)
    private String titulo;

    @Setter
    @Column(nullable = false, columnDefinition = "TEXT")
    private String descricao;

    @Setter
    @Column(nullable = false, length = 30)
    private String status = "ABERTO";

    @Column(name = "data_abertura", updatable = false)
    private LocalDateTime dataAbertura;

    @Setter
    @Column(name = "data_fechamento")
    private LocalDateTime dataFechamento;

    @PrePersist
    protected void onCreate() {
        this.dataAbertura = LocalDateTime.now();
    }
}
