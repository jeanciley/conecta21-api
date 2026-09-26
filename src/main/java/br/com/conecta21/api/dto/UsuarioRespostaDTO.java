package br.com.conecta21.api.dto;

import br.com.conecta21.api.model.PerfilUsuario;

public record UsuarioRespostaDTO(Long id, String nome, String email, PerfilUsuario perfil, boolean ativo) {
}
