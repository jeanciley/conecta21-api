package br.com.conecta21.api.controller;

import br.com.conecta21.api.dto.AuditoriaRespostaDTO;
import br.com.conecta21.api.service.AuditoriaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auditoria")
public class AuditoriaController {

    @Autowired
    private AuditoriaService auditoriaService;

    @GetMapping("/{entidade}/{entidadeId}")
    public ResponseEntity<List<AuditoriaRespostaDTO>> listarHistorico(
            @PathVariable String entidade,
            @PathVariable Long entidadeId) {

        // Exemplo de requisição: GET /api/auditoria/Chamado/42
        return ResponseEntity.ok(auditoriaService.listarHistorico(entidade, entidadeId));
    }
}
