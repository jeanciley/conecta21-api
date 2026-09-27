package br.com.conecta21.api.dto;

public record CategoriaRespostaDTO(
        Long id,
        String nome,
        Long prioridadeId,
        String prioridadeNome
) {
    public CategoriaRespostaDTO(Long id, String nome) { this(id, nome, null, null); }
}
