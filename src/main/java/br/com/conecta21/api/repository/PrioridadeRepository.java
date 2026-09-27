package br.com.conecta21.api.repository;

import br.com.conecta21.api.model.Prioridade;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PrioridadeRepository extends JpaRepository<Prioridade, Long> {
    List<Prioridade> findAllByEmpresaIdOrderByNome(Long empresaId);
    Optional<Prioridade> findByIdAndEmpresaId(Long id, Long empresaId);
}
