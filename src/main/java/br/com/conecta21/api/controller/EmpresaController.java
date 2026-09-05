package br.com.conecta21.api.controller;

import br.com.conecta21.api.dto.EmpresaCadastroDTO;
import br.com.conecta21.api.model.Empresa;
import br.com.conecta21.api.service.EmpresaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/empresas")
public class EmpresaController {

    @Autowired
    private EmpresaService empresaService;

    @PostMapping
    public ResponseEntity<Void> cadastrar(@RequestBody EmpresaCadastroDTO dto) {

        Empresa empresaSalva = empresaService.cadastrarEmpresaComAdmin(dto);

        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(empresaSalva.getId()).toUri();

        return ResponseEntity.created(uri).build();
    }

}
