package br.com.conecta21.api.controller;

import br.com.conecta21.api.dto.PerfilCustomizadoDTO;
import br.com.conecta21.api.service.PerfilCustomizadoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/perfis")
public class PerfilCustomizadoController {
    private final PerfilCustomizadoService service;
    public PerfilCustomizadoController(PerfilCustomizadoService service) { this.service=service; }
    @GetMapping public List<PerfilCustomizadoDTO> listar() { return service.listar(); }
    @PostMapping public ResponseEntity<PerfilCustomizadoDTO> criar(@RequestBody @Valid PerfilCustomizadoDTO dto) { return ResponseEntity.status(201).body(service.salvar(null,dto)); }
    @PutMapping("/{id}") public PerfilCustomizadoDTO atualizar(@PathVariable Long id,@RequestBody @Valid PerfilCustomizadoDTO dto) { return service.salvar(id,dto); }
    @DeleteMapping("/{id}") public ResponseEntity<Void> desativar(@PathVariable Long id) { service.desativar(id); return ResponseEntity.noContent().build(); }
}
