package br.com.conecta21.api.controller;

import br.com.conecta21.api.dto.AnexoRespostaDTO;
import br.com.conecta21.api.dto.ChamadoCriacaoDTO;
import br.com.conecta21.api.dto.ChamadoRespostaDTO;
import br.com.conecta21.api.dto.ChamadoStatusDTO;
import br.com.conecta21.api.service.AnexoChamadoService;
import br.com.conecta21.api.service.ChamadoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/chamados")
public class ChamadoController {

    @Autowired
    private ChamadoService chamadoService;

    @Autowired
    private AnexoChamadoService anexoChamadoService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ChamadoRespostaDTO> criar(@RequestBody @Valid ChamadoCriacaoDTO dto) {
        return respostaCriacao(chamadoService.criar(dto));
    }

    /**
     * Criação com anexos. O part "dados" deve conter JSON com o mesmo payload
     * usado na criação normal e "arquivos" pode conter um ou vários arquivos.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ChamadoRespostaDTO> criarComAnexos(
            @RequestPart("dados") @Valid ChamadoCriacaoDTO dto,
            @RequestPart(value = "arquivos", required = false) List<MultipartFile> arquivos) {
        return respostaCriacao(chamadoService.criar(dto, arquivos));
    }

    @GetMapping
    public ResponseEntity<Page<ChamadoRespostaDTO>> listar(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long tecnicoId,
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dataFim,
            @PageableDefault(size = 20, sort = "dataAbertura", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(chamadoService.listar(status, tecnicoId, dataInicio, dataFim, busca, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChamadoRespostaDTO> detalhar(@PathVariable Long id) {
        return ResponseEntity.ok(chamadoService.detalhar(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ChamadoRespostaDTO> alterarStatus(
            @PathVariable Long id, @RequestBody @Valid ChamadoStatusDTO dto) {
        return ResponseEntity.ok(chamadoService.alterarStatus(id, dto));
    }

    @PostMapping(value = "/{id}/anexos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<AnexoRespostaDTO>> adicionarAnexos(
            @PathVariable Long id,
            @RequestPart("arquivos") List<MultipartFile> arquivos) {
        return ResponseEntity.ok(anexoChamadoService.adicionar(id, arquivos));
    }

    @GetMapping("/{id}/anexos")
    public ResponseEntity<List<AnexoRespostaDTO>> listarAnexos(@PathVariable Long id) {
        return ResponseEntity.ok(anexoChamadoService.listar(id));
    }

    @GetMapping("/{id}/anexos/{anexoId}")
    public ResponseEntity<Resource> baixarAnexo(@PathVariable Long id, @PathVariable Long anexoId) {
        AnexoChamadoService.AnexoDownload download = anexoChamadoService.carregar(id, anexoId);

        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (download.tipoMime() != null && !download.tipoMime().isBlank()) {
            try {
                mediaType = MediaType.parseMediaType(download.tipoMime());
            } catch (IllegalArgumentException ignored) {
                // Mantém application/octet-stream quando o MIME salvo não é válido.
            }
        }

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(download.nomeOriginal(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(download.resource());
    }

    private ResponseEntity<ChamadoRespostaDTO> respostaCriacao(ChamadoRespostaDTO resposta) {
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(resposta.id()).toUri();
        return ResponseEntity.created(uri).body(resposta);
    }
}
