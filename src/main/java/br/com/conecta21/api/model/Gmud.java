package br.com.conecta21.api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "gmuds", indexes = {
        @Index(name = "idx_gmud_empresa_ticket", columnList = "empresa_id,ticket_id"),
        @Index(name = "idx_gmud_responsavel", columnList = "responsavel_id")
})
public class Gmud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Chamado chamado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "responsavel_id", nullable = false)
    private Usuario responsavel;

    @Column(name = "modelo_utilizado", nullable = false, length = 100)
    private String modeloUtilizado;

    @Column(nullable = false, length = 40)
    private String ambiente;

    @Column(name = "data_agendada", nullable = false)
    private LocalDateTime dataAgendada;

    @Column(name = "riscos_impactos", nullable = false, columnDefinition = "TEXT")
    private String riscosImpactos;

    @Column(name = "status_aprovacao", nullable = false, length = 30)
    private String statusAprovacao = "PENDENTE";

    @Column(name = "caminho_arquivo_gerado", length = 500)
    private String caminhoArquivoGerado;

    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @PrePersist
    protected void onCreate() {
        if (dataCriacao == null) dataCriacao = LocalDateTime.now();
        if (statusAprovacao == null || statusAprovacao.isBlank()) statusAprovacao = "PENDENTE";
    }
}
