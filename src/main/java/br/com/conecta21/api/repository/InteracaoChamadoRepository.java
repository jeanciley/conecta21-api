package br.com.conecta21.api.repository;

import br.com.conecta21.api.model.InteracaoChamado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public interface InteracaoChamadoRepository extends JpaRepository<InteracaoChamado, Long> {

    Optional<InteracaoChamado> findByIdAndChamadoIdAndChamadoEmpresaId(
            Long id, Long chamadoId, Long empresaId);

    Page<InteracaoChamado> findAllByChamadoIdAndChamadoEmpresaIdOrderByDataCriacaoAsc(
            Long chamadoId, Long empresaId, Pageable pageable);
}
