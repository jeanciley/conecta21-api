package br.com.conecta21.api.dto;

import jakarta.validation.constraints.NotNull;

public record ChamadoResponsavelAlteracaoDTO(@NotNull Long responsavelId) {
}
