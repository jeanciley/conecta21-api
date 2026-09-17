package br.com.conecta21.api.repository;

import br.com.conecta21.api.model.Chamado;
import br.com.conecta21.api.model.StatusChamado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query(
            value = """
                    SELECT c.*
                    FROM chamados c
                    WHERE c.empresa_id = :empresaId
                      AND MATCH(c.titulo, c.descricao)
                          AGAINST (:busca IN NATURAL LANGUAGE MODE)
                      AND (:status IS NULL OR c.status = :status)
                      AND (:tecnicoId IS NULL OR c.tecnico_id = :tecnicoId)
                      AND (:dataInicio IS NULL OR c.data_abertura >= :dataInicio)
                      AND (:dataFim IS NULL OR c.data_abertura <= :dataFim)
                    ORDER BY MATCH(c.titulo, c.descricao)
                          AGAINST (:busca IN NATURAL LANGUAGE MODE) DESC,
                             c.data_abertura DESC
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM chamados c
                    WHERE c.empresa_id = :empresaId
                      AND MATCH(c.titulo, c.descricao)
                          AGAINST (:busca IN NATURAL LANGUAGE MODE)
                      AND (:status IS NULL OR c.status = :status)
                      AND (:tecnicoId IS NULL OR c.tecnico_id = :tecnicoId)
                      AND (:dataInicio IS NULL OR c.data_abertura >= :dataInicio)
                      AND (:dataFim IS NULL OR c.data_abertura <= :dataFim)
                    """,
            nativeQuery = true)
    Page<Chamado> pesquisarFullText(
            @Param("empresaId") Long empresaId,
            @Param("busca") String busca,
            @Param("status") String status,
            @Param("tecnicoId") Long tecnicoId,
            @Param("dataInicio") LocalDateTime dataInicio,
            @Param("dataFim") LocalDateTime dataFim,
            Pageable pageable);
}
