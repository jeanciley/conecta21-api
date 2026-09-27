package br.com.conecta21.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PrioridadeDTO(Long id, @NotBlank @Size(max = 50) String nome,
                            @NotNull @Min(1) Integer slaRespostaMinutos,
                            @NotNull @Min(1) Integer slaResolucaoMinutos, boolean ativa) {}
