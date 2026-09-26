package br.com.conecta21.api.dto;

import br.com.conecta21.api.model.PerfilUsuario;
import jakarta.validation.constraints.*;

public record UsuarioCadastroDTO(
        @NotBlank @Size(max = 100) String nome,
        @NotBlank @Email @Size(max = 100) String email,
        @NotNull PerfilUsuario perfil
) {
}
