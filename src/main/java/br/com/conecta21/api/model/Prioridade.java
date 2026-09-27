package br.com.conecta21.api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "prioridades", uniqueConstraints = @UniqueConstraint(name = "uk_prioridade_empresa_nome", columnNames = {"empresa_id", "nome"}))
public class Prioridade {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;
    @Column(nullable = false, length = 50)
    private String nome;
    @Column(name = "sla_resposta_minutos", nullable = false)
    private Integer slaRespostaMinutos;
    @Column(name = "sla_resolucao_minutos", nullable = false)
    private Integer slaResolucaoMinutos;
    @Column(nullable = false)
    private boolean ativa = true;
}
