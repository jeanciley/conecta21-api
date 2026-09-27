package br.com.conecta21.api.controller;

import br.com.conecta21.api.dto.InteracaoCriacaoDTO;
import br.com.conecta21.api.dto.InteracaoRespostaDTO;
import br.com.conecta21.api.dto.AnexoArquivoDTO;
import br.com.conecta21.api.service.InteracaoChamadoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ContentDisposition;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestPart;

@RestController
@RequestMapping("/api/chamados/{chamadoId}/interacoes")
public class InteracaoChamadoController {

    @Autowired
    private InteracaoChamadoService interacaoService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InteracaoRespostaDTO> comentar(
            @PathVariable Long chamadoId,
            @RequestBody @Valid InteracaoCriacaoDTO dto) {
        InteracaoRespostaDTO resposta = interacaoService.comentar(chamadoId, dto);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(resposta.id()).toUri();
        return ResponseEntity.created(uri).body(resposta);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<InteracaoRespostaDTO> comentarComAnexos(
            @PathVariable Long chamadoId,
            @RequestPart("mensagem") String mensagem,
            @RequestPart(value = "tipo", required = false) String tipo,
            @RequestPart(value = "imagens", required = false) List<MultipartFile> imagens) {
        InteracaoRespostaDTO resposta = interacaoService.comentar(
                chamadoId, new InteracaoCriacaoDTO(mensagem, tipo), imagens);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(resposta.id()).toUri();
        return ResponseEntity.created(uri).body(resposta);
    }

    @GetMapping("/{interacaoId}/anexos/{anexoId}")
    public ResponseEntity<byte[]> baixarAnexo(
            @PathVariable Long chamadoId,
            @PathVariable Long interacaoId,
            @PathVariable Long anexoId) {
        AnexoArquivoDTO anexo = interacaoService.baixarAnexo(chamadoId, interacaoId, anexoId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(anexo.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(anexo.nomeArquivo(), StandardCharsets.UTF_8).build().toString())
                .body(anexo.conteudo());
    }

    @GetMapping("/{interacaoId}")
    public ResponseEntity<InteracaoRespostaDTO> detalhar(
            @PathVariable Long chamadoId, @PathVariable Long interacaoId) {
        return ResponseEntity.ok(interacaoService.detalhar(chamadoId, interacaoId));
    }

    @GetMapping
    public ResponseEntity<Page<InteracaoRespostaDTO>> listar(
            @PathVariable Long chamadoId,
            @PageableDefault(size = 20, sort = "dataCriacao", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(interacaoService.listar(chamadoId, pageable));
    }
}
