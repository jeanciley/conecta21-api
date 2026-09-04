package br.com.conecta21.api.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "chamados")
public class Chamado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitante_id", nullable = false)
    private Usuario solicitante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tecnico_id")
    private Usuario tecnico;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descricao;

    @Column(nullable = false, length = 30)
    private String status = "ABERTO";

    @Column(name = "data_abertura", updatable = false)
    private LocalDateTime dataAbertura;

    @Column(name = "data_fechamento")
    private LocalDateTime dataFechamento;

    @PrePersist
    protected void onCreate() {
        this.dataAbertura = LocalDateTime.now();
    }
}
