package br.com.conecta21.api.repository;

import br.com.conecta21.api.model.Gmud;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GmudRepository extends JpaRepository<Gmud, Long> {
    List<Gmud> findAllByChamadoIdAndEmpresaIdOrderByDataCriacaoDesc(Long ticketId, Long empresaId);
    Optional<Gmud> findByIdAndEmpresaId(Long id, Long empresaId);
}
