package br.com.conecta21.api.repository;

import br.com.conecta21.api.model.Chamado;
import br.com.conecta21.api.model.StatusChamado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChamadoRepository extends JpaRepository<Chamado, Long>, JpaSpecificationExecutor<Chamado> {

    List<Chamado> findAllByEmpresaId(Long empresaId);

    Optional<Chamado> findByIdAndEmpresaId(Long id, Long empresaId);

    long countByEmpresaIdAndStatusAndDataAberturaAfter(
            Long empresaId,
            StatusChamado status,
            LocalDateTime dataLimite
    );

    List<Chamado> findByStatusInAndDataLimiteResolucaoBefore(
            List<StatusChamado> status,
            LocalDateTime momentoAtual
    );

    List<Chamado> findAllByEmpresaIdAndSolicitanteId(Long empresaId, Long solicitanteId);
}
