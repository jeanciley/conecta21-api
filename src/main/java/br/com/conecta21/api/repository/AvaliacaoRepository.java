package br.com.conecta21.api.repository;

import br.com.conecta21.api.model.Avaliacao;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

@Repository
public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {

    Optional<Avaliacao> findByChamadoIdAndChamadoEmpresaId(Long chamadoId, Long empresaId);

    boolean existsByChamadoId(Long chamadoId);
}
