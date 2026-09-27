package br.com.conecta21.api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Entity
@Table(name = "perfis_customizados", uniqueConstraints = @UniqueConstraint(name = "uk_perfil_empresa_nome", columnNames = {"empresa_id", "nome"}))
public class PerfilCustomizado {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "empresa_id", nullable = false) private Empresa empresa;
    @Column(nullable = false, length = 60) private String nome;
    @Column(length = 255) private String descricao;
    @Column(nullable = false, columnDefinition = "TEXT") private String permissoes = "";
    @Column(nullable = false) private boolean ativo = true;
}
