package br.com.conecta21.api.repository;

import br.com.conecta21.api.model.EmpresaModulo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface EmpresaModuloRepository extends JpaRepository<EmpresaModulo, Long> {
    List<EmpresaModulo> findAllByEmpresaId(Long empresaId);
    Optional<EmpresaModulo> findByEmpresaIdAndCodigo(Long empresaId, String codigo);
}
