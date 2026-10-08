package br.com.conecta21.api.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record GmudCriacaoDTO(
        @NotNull Long responsavelId,
        @NotBlank @Size(max = 100) String modeloUtilizado,
        @NotBlank @Size(max = 40) String ambiente,
        @NotNull @FutureOrPresent LocalDateTime dataAgendada,
        @NotBlank @Size(max = 10000) String riscosImpactos
) {}
