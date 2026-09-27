package br.com.conecta21.api.controller;

import br.com.conecta21.api.dto.PrioridadeDTO;
import br.com.conecta21.api.service.PrioridadeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/prioridades")
public class PrioridadeController {
    private final PrioridadeService service;
    public PrioridadeController(PrioridadeService service) { this.service = service; }
    @GetMapping public List<PrioridadeDTO> listar(@RequestParam(defaultValue = "false") boolean somenteAtivas) { return service.listar(somenteAtivas); }
    @PostMapping public ResponseEntity<PrioridadeDTO> criar(@RequestBody @Valid PrioridadeDTO dto) { return ResponseEntity.status(201).body(service.salvar(null, dto)); }
    @PutMapping("/{id}") public PrioridadeDTO atualizar(@PathVariable Long id, @RequestBody @Valid PrioridadeDTO dto) { return service.salvar(id, dto); }
}
