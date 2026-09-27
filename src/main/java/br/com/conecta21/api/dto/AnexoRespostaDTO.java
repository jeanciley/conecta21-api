package br.com.conecta21.api.dto;

import java.time.LocalDateTime;

public record AnexoRespostaDTO(
        Long id,
        String nomeOriginal,
        String tipoMime,
        Long tamanhoBytes,
        LocalDateTime dataUpload
) {
}
