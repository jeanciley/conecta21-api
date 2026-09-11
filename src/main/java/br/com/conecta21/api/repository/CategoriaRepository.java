package br.com.conecta21.api.repository;

import br.com.conecta21.api.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    List<Categoria> findAllByEmpresaId(Long empresaId);
}
