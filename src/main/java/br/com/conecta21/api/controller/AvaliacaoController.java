package br.com.conecta21.api.controller;

import br.com.conecta21.api.dto.AvaliacaoCriacaoDTO;
import br.com.conecta21.api.dto.AvaliacaoRespostaDTO;
import br.com.conecta21.api.service.AvaliacaoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/chamados/{chamadoId}/avaliacao")
public class AvaliacaoController {

    @Autowired
    private AvaliacaoService avaliacaoService;

    @PostMapping
    public ResponseEntity<AvaliacaoRespostaDTO> criar(
            @PathVariable Long chamadoId,
            @RequestBody @Valid AvaliacaoCriacaoDTO dto) {
        AvaliacaoRespostaDTO resposta = avaliacaoService.criar(chamadoId, dto);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .build()
                .toUri();
        return ResponseEntity.created(uri).body(resposta);
    }

    @GetMapping
    public ResponseEntity<AvaliacaoRespostaDTO> buscar(@PathVariable Long chamadoId) {
        return ResponseEntity.ok(avaliacaoService.buscar(chamadoId));
    }
}
