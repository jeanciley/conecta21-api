package br.com.conecta21.api.controller;

import br.com.conecta21.api.dto.ModuloDTO;
import br.com.conecta21.api.dto.ModulosRespostaDTO;
import br.com.conecta21.api.service.ModuloService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/modulos")
public class ModuloController {
    private final ModuloService moduloService;

    public ModuloController(ModuloService moduloService) { this.moduloService = moduloService; }

    @GetMapping
    public ModulosRespostaDTO listar() { return moduloService.listar(); }

    @PostMapping("/{codigo}/contratar")
    public ModuloDTO contratar(@PathVariable String codigo) { return moduloService.contratar(codigo); }
}
