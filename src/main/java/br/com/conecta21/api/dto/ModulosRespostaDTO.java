package br.com.conecta21.api.dto;

import java.util.List;

public record ModulosRespostaDTO(boolean gmudAtivo, List<ModuloDTO> modulos) {}
