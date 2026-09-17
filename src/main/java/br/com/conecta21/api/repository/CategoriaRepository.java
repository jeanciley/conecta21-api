package br.com.conecta21.api.repository;

import br.com.conecta21.api.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    List<Categoria> findAllByEmpresaId(Long empresaId);

    Optional<Categoria> findByIdAndEmpresaId(Long id, Long empresaId);
}
