package br.com.conecta21.api.controller;

import br.com.conecta21.api.dto.ArtigoCriacaoDTO;
import br.com.conecta21.api.dto.ArtigoRespostaDTO;
import br.com.conecta21.api.service.ArtigoFaqService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/artigos")
public class ArtigoFaqController {

    @Autowired
    private ArtigoFaqService artigoFaqService;

    @PostMapping
    public ResponseEntity<ArtigoRespostaDTO> criar(@RequestBody @Valid ArtigoCriacaoDTO dto) {

        ArtigoRespostaDTO resposta = artigoFaqService.criar(dto);

        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(resposta.id()).toUri();

        return ResponseEntity.created(uri).body(resposta);
    }

    // CORREÇÃO: O retorno foi alterado de Page para List, e a lógica
    // roteia para o método correto do Service dependendo se há um termo de busca.
    @GetMapping
    public ResponseEntity<List<ArtigoRespostaDTO>> listar(@RequestParam(required = false) String busca) {
        if (busca != null && !busca.isBlank()) {
            return ResponseEntity.ok(artigoFaqService.buscarPorTermo(busca));
        }
        return ResponseEntity.ok(artigoFaqService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ArtigoRespostaDTO> detalhar(@PathVariable Long id) {
        return ResponseEntity.ok(artigoFaqService.detalhar(id));
    }
}