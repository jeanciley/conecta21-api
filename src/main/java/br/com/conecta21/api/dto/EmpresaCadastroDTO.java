package br.com.conecta21.api.dto;

public record EmpresaCadastroDTO(
        String nomeFantasia,
        String cnpj,
        String nomeUsuario,
        String emailUsuario,
        String senhaUsuario
) {}
