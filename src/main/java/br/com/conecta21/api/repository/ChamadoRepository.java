package br.com.conecta21.api.repository;

import br.com.conecta21.api.model.Chamado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChamadoRepository extends JpaRepository<Chamado, Long> {

    List<Chamado> findAllByEmpresaId(Long empresaId);
}
