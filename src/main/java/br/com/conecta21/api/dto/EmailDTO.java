package br.com.conecta21.api.dto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
public record EmailDTO(@NotBlank @Email String email) {}
