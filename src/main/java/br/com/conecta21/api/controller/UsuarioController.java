package br.com.conecta21.api.controller;

import br.com.conecta21.api.dto.UsuarioCadastroDTO;
import br.com.conecta21.api.dto.UsuarioRespostaDTO;
import br.com.conecta21.api.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<Void> cadastrar(@RequestBody @Valid UsuarioCadastroDTO dto){
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
}
