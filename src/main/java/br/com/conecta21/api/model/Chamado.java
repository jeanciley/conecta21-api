package br.com.conecta21.api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "chamados")
// 1. Intercepta o delete físico e transforma em update
@SQLDelete(sql = "UPDATE chamados SET excluido = true WHERE id = ?")
// 2. Filtra automaticamente os excluídos em todas as consultas (substitui o antigo @Where)
@SQLRestriction("excluido = false")
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PrioridadeChamado prioridade;

    @Column(name = "data_limite_resolucao")
    private LocalDateTime dataLimiteResolucao;

    @Column(nullable = false)
    private boolean excluido = false;

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

        if (this.prioridade != null) {
            this.dataLimiteResolucao = this.dataAbertura.plusHours(this.prioridade.getHorasSla());
        }
    }
}
