package br.com.conecta21.api.repository;

import br.com.conecta21.api.model.LogAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LogAuditoriaRepository extends JpaRepository<LogAuditoria, Long> {
    List<LogAuditoria> findByEmpresaIdAndEntidadeAndEntidadeIdOrderByDataCriacaoDesc(Long empresaId, String entidade, Long entidadeId);
}
