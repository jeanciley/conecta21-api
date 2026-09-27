package br.com.conecta21.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record PerfilCustomizadoDTO(Long id, @NotBlank @Size(max = 60) String nome,
                                   @Size(max = 255) String descricao, Set<String> permissoes, boolean ativo) {}
