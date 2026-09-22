package br.com.conecta21.api.controller;

import br.com.conecta21.api.dto.*;
import br.com.conecta21.api.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @PostMapping
    public ResponseEntity<Void> cadastrar(@RequestBody @Valid UsuarioCadastroDTO dto) {
        usuarioService.cadastrarMembro(dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/me")
    public ResponseEntity<UsuarioPerfilRespostaDTO> meuPerfil() {
        return ResponseEntity.ok(usuarioService.meuPerfil());
    }

    @PatchMapping("/me")
    public ResponseEntity<UsuarioPerfilRespostaDTO> atualizarMeuPerfil(
            @RequestBody @Valid UsuarioPerfilAtualizacaoDTO dto) {
        return ResponseEntity.ok(usuarioService.atualizarMeuPerfil(dto));
    }

    @PatchMapping("/me/senha")
    public ResponseEntity<Void> trocarMinhaSenha(@RequestBody @Valid UsuarioTrocaSenhaDTO dto) {
        usuarioService.trocarMinhaSenha(dto);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UsuarioPerfilRespostaDTO> atualizarAvatar(@RequestPart("arquivo") MultipartFile arquivo) {
        return ResponseEntity.ok(usuarioService.atualizarAvatar(arquivo));
    }

    @GetMapping("/me/avatar")
    public ResponseEntity<Resource> carregarAvatar() {
        UsuarioService.AvatarDownload download = usuarioService.carregarMeuAvatar();

        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (download.tipoMime() != null && !download.tipoMime().isBlank()) {
            try {
                mediaType = MediaType.parseMediaType(download.tipoMime());
            } catch (IllegalArgumentException ignored) {
                // Mantém octet-stream caso o MIME persistido não seja válido.
            }
        }

        String nome = download.nomeOriginal() == null ? "avatar" : download.nomeOriginal();
        ContentDisposition disposition = ContentDisposition.inline()
                .filename(nome, StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(download.resource());
    }

    @DeleteMapping("/me/avatar")
    public ResponseEntity<Void> removerAvatar() {
        usuarioService.removerMeuAvatar();
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<UsuarioListaDTO>> listarMembros() {
        return ResponseEntity.ok(usuarioService.listarMembrosDaEmpresa());
    }
}
