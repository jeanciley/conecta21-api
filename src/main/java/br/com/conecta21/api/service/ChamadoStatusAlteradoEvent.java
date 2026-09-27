package br.com.conecta21.api.service;

import br.com.conecta21.api.model.StatusChamado;

public record ChamadoStatusAlteradoEvent(
        Long chamadoId,
        String titulo,
        String emailSolicitante,
        String nomeSolicitante,
        StatusChamado statusAnterior,
        StatusChamado statusNovo
) {
}
