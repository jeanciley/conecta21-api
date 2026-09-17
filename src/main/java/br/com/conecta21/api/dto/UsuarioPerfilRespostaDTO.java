package br.com.conecta21.api.dto;

import br.com.conecta21.api.model.PerfilUsuario;

public record UsuarioPerfilRespostaDTO(
        Long id,
        Long empresaId,
        String nome,
        String email,
        PerfilUsuario perfil,
        boolean possuiAvatar
) {
}
