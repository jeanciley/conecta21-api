package br.com.conecta21.api.controller;

import br.com.conecta21.api.dto.ArtigoCriacaoDTO;
import br.com.conecta21.api.dto.ArtigoRespostaDTO;
import br.com.conecta21.api.service.ArtigoFaqService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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

    @GetMapping
    public ResponseEntity<Page<ArtigoRespostaDTO>> listar(
            @RequestParam(required = false) String busca,
            @PageableDefault(size = 20, sort = "dataCriacao", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(artigoFaqService.listar(busca, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ArtigoRespostaDTO> detalhar(@PathVariable Long id) {
        return ResponseEntity.ok(artigoFaqService.detalhar(id));
    }
}
