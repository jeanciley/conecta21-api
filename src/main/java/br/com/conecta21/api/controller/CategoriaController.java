package br.com.conecta21.api.controller;

import br.com.conecta21.api.model.Categoria;
import br.com.conecta21.api.repository.CategoriaRepository;
import br.com.conecta21.api.security.TenantContext;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/categorias")
public class CategoriaController {

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private TenantContext tenantContext;

    @GetMapping
    public ResponseEntity<List<Categoria>> listar() {
        // Retorna apenas as categorias da empresa logada
        return ResponseEntity.ok(categoriaRepository.findAllByEmpresaId(tenantContext.getEmpresaIdAutenticada()));
    }

    @PostMapping
    public ResponseEntity<Categoria> criar(@RequestBody @NotBlank String nome) {
        Categoria categoria = new Categoria();
        categoria.setNome(nome);
        // Associa obrigatoriamente à empresa logada (Multi-tenant)
        categoria.setEmpresa(tenantContext.getUsuarioAutenticado().getEmpresa());

        Categoria salva = categoriaRepository.save(categoria);
        return ResponseEntity.status(HttpStatus.CREATED).body(salva);
    }
}
