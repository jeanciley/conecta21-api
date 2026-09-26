package br.com.conecta21.api.controller;

import br.com.conecta21.api.dto.CategoriaCriacaoDTO;
import br.com.conecta21.api.dto.CategoriaRespostaDTO;
import br.com.conecta21.api.service.CategoriaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("api/categorias")
public class CategoriaController {

    @Autowired
    private CategoriaService categoriaService;

    @PostMapping
    public ResponseEntity<CategoriaRespostaDTO> criar(@RequestBody @Valid CategoriaCriacaoDTO dto) {
        CategoriaRespostaDTO resposta = categoriaService.criar(dto);

        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(resposta.id()).toUri();

        return ResponseEntity.created(uri).body(resposta);
    }

    @GetMapping
    public ResponseEntity<List<CategoriaRespostaDTO>> listar() {
        // Retorna a lista de DTOs com status 200 OK
        return ResponseEntity.ok(categoriaService.listar());
    }
}
