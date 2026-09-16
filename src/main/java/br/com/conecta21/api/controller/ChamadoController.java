package br.com.conecta21.api.controller;

import br.com.conecta21.api.dto.ChamadoCriacaoDTO;
import br.com.conecta21.api.dto.ChamadoRespostaDTO;
import br.com.conecta21.api.dto.ChamadoStatusDTO;
import br.com.conecta21.api.dto.KanbanResponseDTO;
import br.com.conecta21.api.service.ChamadoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/chamados")
public class ChamadoController {

    @Autowired
    private ChamadoService chamadoService;

    @PostMapping
    public ResponseEntity<ChamadoRespostaDTO> criar(@RequestBody @Valid ChamadoCriacaoDTO dto) {
        ChamadoRespostaDTO resposta = chamadoService.criar(dto);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(resposta.id()).toUri();
        return ResponseEntity.created(uri).body(resposta);
    }

    @GetMapping
    public ResponseEntity<Page<ChamadoRespostaDTO>> listar(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long tecnicoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataFim,
            @PageableDefault(size = 20, sort = "dataAbertura", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(chamadoService.listar(status, tecnicoId, dataInicio, dataFim, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChamadoRespostaDTO> detalhar(@PathVariable Long id) {
        return ResponseEntity.ok(chamadoService.detalhar(id));
    }

    @GetMapping("/kanban")
    public ResponseEntity<KanbanResponseDTO> obterKanban(
            @RequestParam(required = false) Long tecnicoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataFim,
            @RequestParam(required = false, defaultValue = "50") Integer limite) {
        return ResponseEntity.ok(chamadoService.obterKanban(tecnicoId, dataInicio, dataFim, limite));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ChamadoRespostaDTO> alterarStatus(
            @PathVariable Long id, @RequestBody @Valid ChamadoStatusDTO dto) {
        return ResponseEntity.ok(chamadoService.alterarStatus(id, dto));
    }
}
