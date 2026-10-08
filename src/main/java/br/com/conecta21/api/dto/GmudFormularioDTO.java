package br.com.conecta21.api.dto;

import br.com.conecta21.api.service.TemplateService;
import java.util.List;

public record GmudFormularioDTO(
        Long ticketId, String numeroChamado, String cliente, String solicitante, String tecnicoAtribuido,
        Long responsavelSugeridoId, List<String> modelos, List<GmudResponsavelDTO> responsaveis,
        List<TemplateService.TemplateInfo> templates
) {}
