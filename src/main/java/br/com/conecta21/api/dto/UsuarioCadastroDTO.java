package br.com.conecta21.api.dto;

import br.com.conecta21.api.model.PerfilUsuario;
import jakarta.validation.constraints.*;

public record UsuarioCadastroDTO(
        @NotBlank String nome,
        @NotBlank @Email String email,
        @NotBlank
        @Pattern(
                regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{8,}$",
                message = "A senha deve ter no mínimo 8 caracteres, contendo pelo menos uma letra maiúscula, uma minúscula, um número e um caractere especial"
        ) String senha,
        @NotNull PerfilUsuario perfil
) {
}
