package br.com.conecta21.api.dto;

import java.time.LocalDateTime;

public record ModuloDTO(String codigo, String nome, String descricao, boolean contratado, LocalDateTime contratadoEm) {}
