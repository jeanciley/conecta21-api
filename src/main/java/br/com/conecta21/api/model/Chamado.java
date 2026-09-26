package br.com.conecta21.api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
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

    @Enumerated(EnumType.STRING)
    private StatusChamado status;

    @Column(name = "data_abertura", updatable = false)
    private LocalDateTime dataAbertura;

    @Setter
    @Column(name = "data_fechamento")
    private LocalDateTime dataFechamento;

    @Column(nullable = false, length = 50)
    private String prioridade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prioridade_id")
    private Prioridade prioridadeConfigurada;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @Column(name = "sla_resposta_minutos_snapshot")
    private Integer slaRespostaMinutosSnapshot;

    @Column(name = "sla_resolucao_minutos_snapshot")
    private Integer slaResolucaoMinutosSnapshot;

    @Column(name = "data_limite_resposta")
    private LocalDateTime dataLimiteResposta;

    @Column(name = "data_primeira_resposta")
    private LocalDateTime dataPrimeiraResposta;

    @Column(name = "data_limite_resolucao")
    private LocalDateTime dataLimiteResolucao;

    @ManyToMany
    @JoinTable(
            name = "chamado_categoria", // Nome da tabela intermediária que será criada no banco
            joinColumns = @JoinColumn(name = "chamado_id"), // A chave pro chamado
            inverseJoinColumns = @JoinColumn(name = "categoria_id") // A chave pra categoria
    )
    private java.util.Set<Categoria> categorias = new java.util.HashSet<>();

    @PrePersist
    protected void onCreate() {

        this.dataAbertura = LocalDateTime.now();

        if (this.status == null) {
            this.status = StatusChamado.ABERTO;
        }

        if (this.prioridadeConfigurada != null) {
            this.dataLimiteResposta = this.dataAbertura.plusMinutes(this.slaRespostaMinutosSnapshot);
            this.dataLimiteResolucao = this.dataAbertura.plusMinutes(this.slaResolucaoMinutosSnapshot);
        }
    }
}
