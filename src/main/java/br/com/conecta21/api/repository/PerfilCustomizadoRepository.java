package br.com.conecta21.api.repository;

import br.com.conecta21.api.model.PerfilCustomizado;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PerfilCustomizadoRepository extends JpaRepository<PerfilCustomizado, Long> {
    List<PerfilCustomizado> findAllByEmpresaIdOrderByNome(Long empresaId);
    Optional<PerfilCustomizado> findByIdAndEmpresaId(Long id, Long empresaId);
}
