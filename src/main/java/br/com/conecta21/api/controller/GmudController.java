package br.com.conecta21.api.controller;

import br.com.conecta21.api.dto.GmudCriacaoDTO;
import br.com.conecta21.api.dto.GmudFormularioDTO;
import br.com.conecta21.api.dto.GmudRespostaDTO;
import br.com.conecta21.api.service.GmudService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/gmuds")
public class GmudController {

    private final GmudService gmudService;

    public GmudController(GmudService gmudService) {
        this.gmudService = gmudService;
    }

    @GetMapping("/chamados/{ticketId}/formulario")
    public ResponseEntity<GmudFormularioDTO> obterFormulario(@PathVariable Long ticketId) {
        return ResponseEntity.ok(gmudService.obterFormulario(ticketId));
    }

    @PostMapping(value = "/chamados/{ticketId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GmudRespostaDTO> criar(
            @PathVariable Long ticketId,
            @RequestPart("dados") @Valid GmudCriacaoDTO dados,
            @RequestPart(value = "evidencias", required = false) List<MultipartFile> evidencias) {
        return ResponseEntity.ok(gmudService.criar(ticketId, dados, evidencias));
    }

    @PostMapping(value = "/chamados/{ticketId}/gerar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GmudRespostaDTO> criarComTemplate(
            @PathVariable Long ticketId,
            @RequestPart("request") @Valid br.com.conecta21.api.dto.GmudTemplateRequest request,
            @RequestPart(value = "attachments", required = false) List<MultipartFile> attachments,
            @RequestPart(value = "prints", required = false) List<MultipartFile> prints,
            @RequestPart(value = "tasyAppManagerPrint", required = false) MultipartFile appManagerPrint,
            @RequestPart(value = "tasyIntegrationPrint", required = false) MultipartFile integrationPrint,
            @RequestPart(value = "tasyTomcatPrint", required = false) MultipartFile tomcatPrint) {
        return ResponseEntity.ok(gmudService.criarComTemplate(ticketId, request, attachments, prints,
                appManagerPrint, integrationPrint, tomcatPrint));
    }

    @GetMapping("/{gmudId}/arquivo")
    public ResponseEntity<Resource> baixarArquivo(@PathVariable Long gmudId) {
        GmudService.GmudDownload arquivo = gmudService.baixarArquivo(gmudId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(arquivo.mimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(arquivo.nomeArquivo(), StandardCharsets.UTF_8).build().toString())
                .body(arquivo.resource());
    }
}
