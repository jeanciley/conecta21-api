package br.com.conecta21.api.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PrioridadeChamado {
    ALTA(4),
    MEDIA(24),
    BAIXA(48);

    private final int horasSla;

}
