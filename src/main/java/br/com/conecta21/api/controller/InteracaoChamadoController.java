package br.com.conecta21.api.controller;

import br.com.conecta21.api.dto.InteracaoCriacaoDTO;
import br.com.conecta21.api.dto.InteracaoRespostaDTO;
import br.com.conecta21.api.service.InteracaoChamadoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
@RequestMapping("/api/chamados/{chamadoId}/interacoes")
public class InteracaoChamadoController {

    @Autowired
    private InteracaoChamadoService interacaoService;

    @PostMapping
    public ResponseEntity<InteracaoRespostaDTO> comentar(
            @PathVariable Long chamadoId,
            @RequestBody @Valid InteracaoCriacaoDTO dto) {
        InteracaoRespostaDTO resposta = interacaoService.comentar(chamadoId, dto);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(resposta.id()).toUri();
        return ResponseEntity.created(uri).body(resposta);
    }

    @GetMapping
    public ResponseEntity<Page<InteracaoRespostaDTO>> listar(
            @PathVariable Long chamadoId,
            @PageableDefault(size = 20, sort = "dataCriacao", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(interacaoService.listar(chamadoId, pageable));
    }
}
