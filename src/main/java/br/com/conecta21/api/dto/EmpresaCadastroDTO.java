package br.com.conecta21.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EmpresaCadastroDTO(
        @NotBlank @Size(max = 100) String nomeFantasia,
        @Size(max = 18) String cnpj,
        @NotBlank @Size(max = 100) String nomeUsuario,
        @NotBlank @Email @Size(max = 100) String emailUsuario,
        @NotBlank
        @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!]).{8,}$",
                message = "A senha deve ter no mínimo 8 caracteres, incluindo maiúscula, minúscula, número e caractere especial")
        String senhaUsuario
) {}
