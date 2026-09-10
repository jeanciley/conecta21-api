package br.com.conecta21.api.repository;

import br.com.conecta21.api.model.AnexoChamado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnexoChamadoRepository extends JpaRepository<AnexoChamado, Long> {

    List<AnexoChamado> findAllByChamadoIdAndChamadoEmpresaIdOrderByDataUploadAsc(
            Long chamadoId,
            Long empresaId
    );

    Optional<AnexoChamado> findByIdAndChamadoIdAndChamadoEmpresaId(
            Long anexoId,
            Long chamadoId,
            Long empresaId
    );
}
