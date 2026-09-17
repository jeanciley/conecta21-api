package br.com.conecta21.api.dto;

import java.util.List;

public record KanbanResponseDTO(
        List<KanbanCardDTO> abertos,
        List<KanbanCardDTO> emAndamento,
        List<KanbanCardDTO> emAtraso,
        List<KanbanCardDTO> resolvidos
) {
}
