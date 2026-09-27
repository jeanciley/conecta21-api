package br.com.conecta21.api.controller;

import br.com.conecta21.api.dto.UsuarioCadastroDTO;
import br.com.conecta21.api.dto.UsuarioPerfilAtualizacaoDTO;
import br.com.conecta21.api.dto.UsuarioPerfilRespostaDTO;
import br.com.conecta21.api.dto.UsuarioRespostaDTO;
import br.com.conecta21.api.dto.UsuarioTrocaSenhaDTO;
import br.com.conecta21.api.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
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

    @GetMapping
    public ResponseEntity<List<UsuarioRespostaDTO>> listar() {
        return ResponseEntity.ok(usuarioService.listarMembros());
    }

    @GetMapping("/me")
    public ResponseEntity<UsuarioRespostaDTO> obterPerfilLogado() {
        return ResponseEntity.ok(usuarioService.obterPerfilLogado());
    }

    @GetMapping("/me/perfil")
    public ResponseEntity<UsuarioPerfilRespostaDTO> meuPerfil() {
        return ResponseEntity.ok(usuarioService.meuPerfil());
    }

    @PutMapping("/me/perfil")
    public ResponseEntity<UsuarioPerfilRespostaDTO> atualizarMeuPerfil(
            @RequestBody @Valid UsuarioPerfilAtualizacaoDTO dto) {
        return ResponseEntity.ok(usuarioService.atualizarMeuPerfil(dto));
    }

    @PutMapping("/me/senha")
    public ResponseEntity<Void> trocarMinhaSenha(@RequestBody @Valid UsuarioTrocaSenhaDTO dto) {
        usuarioService.trocarMinhaSenha(dto);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UsuarioPerfilRespostaDTO> atualizarMeuAvatar(@RequestParam("arquivo") MultipartFile arquivo) {
        return ResponseEntity.ok(usuarioService.atualizarAvatar(arquivo));
    }

    @GetMapping("/me/avatar")
    public ResponseEntity<Resource> baixarMeuAvatar() {
        UsuarioService.AvatarDownload avatar = usuarioService.carregarMeuAvatar();
        MediaType tipo = MediaType.APPLICATION_OCTET_STREAM;
        if (avatar.tipoMime() != null && !avatar.tipoMime().isBlank()) {
            try {
                tipo = MediaType.parseMediaType(avatar.tipoMime());
            } catch (IllegalArgumentException ignored) {
                // Usa octet-stream se o MIME persistido estiver inválido.
            }
        }
        return ResponseEntity.ok().contentType(tipo)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(avatar.nomeOriginal()).build().toString())
                .body(avatar.resource());
    }

    @DeleteMapping("/me/avatar")
    public ResponseEntity<Void> removerMeuAvatar() {
        usuarioService.removerMeuAvatar();
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        usuarioService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
