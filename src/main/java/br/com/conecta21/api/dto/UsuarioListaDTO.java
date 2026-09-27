package br.com.conecta21.api.dto;

import br.com.conecta21.api.model.PerfilUsuario;
import br.com.conecta21.api.model.Usuario;

public record UsuarioListaDTO(
        Long id,
        String nome,
        String email,
        PerfilUsuario perfil,
        String status
) {
    public UsuarioListaDTO(Usuario usuario) {
        // Assume "Ativo" por padrão caso não tenha um campo de status na entidade
        this(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getPerfil(), "Ativo");
    }
}