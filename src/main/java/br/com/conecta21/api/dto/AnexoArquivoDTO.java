package br.com.conecta21.api.dto;

public record AnexoArquivoDTO(String nomeArquivo, String contentType, byte[] conteudo) {
}
