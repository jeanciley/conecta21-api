package br.com.conecta21.api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "empresa_modulos", uniqueConstraints = @UniqueConstraint(name = "uk_empresa_modulo_codigo", columnNames = {"empresa_id", "codigo"}))
public class EmpresaModulo {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(nullable = false, length = 60)
    private String codigo;

    @Column(nullable = false)
    private boolean ativo;

    @Column(name = "contratado_em")
    private LocalDateTime contratadoEm;
}
