package br.com.conecta21.api.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "usuarios")
public class Usuario implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String senha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PerfilUsuario perfil;

    @Column(name = "data_criacao", updatable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "ativo", nullable = false)
    private boolean ativo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "perfil_customizado_id")
    private PerfilCustomizado perfilCustomizado;

    @Column(name = "excluido_em")
    private LocalDateTime excluidoEm;

    @PrePersist
    protected void onCreate() {
        this.dataCriacao = LocalDateTime.now();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Agora extraímos o nome da constante do Enum
        List<SimpleGrantedAuthority> authorities = new java.util.ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + this.perfil.name()));
        if (perfilCustomizado != null) {
            authorities.add(new SimpleGrantedAuthority("ROLE_CUSTOM"));
            for (String permission : perfilCustomizado.getPermissoes().split(",")) {
                if (!permission.isBlank()) authorities.add(new SimpleGrantedAuthority("PERM_" + permission.trim()));
            }
        }
        return authorities;
    }

    public boolean temPermissao(String permissao) {
        if (perfil == PerfilUsuario.ADMIN) return true;
        if (perfil == PerfilUsuario.TECNICO && ("CHAMADOS_INTERNOS".equals(permissao) || "GERAR_RELATORIOS".equals(permissao))) return true;
        return perfilCustomizado != null && perfilCustomizado.isAtivo()
                && java.util.Arrays.stream(perfilCustomizado.getPermissoes().split(",")).anyMatch(permissao::equals);
    }

    @Override
    public String getPassword() {
        return this.senha;
    }

    @Override
    public String getUsername() {
        return this.email;
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return ativo && excluidoEm == null; }
}
