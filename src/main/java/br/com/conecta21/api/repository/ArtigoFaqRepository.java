package br.com.conecta21.api.repository;

import br.com.conecta21.api.model.ArtigoFaq;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArtigoFaqRepository extends JpaRepository<ArtigoFaq, Long> {

    List<ArtigoFaq> findAllByEmpresaId(Long empresaId);

    Optional<ArtigoFaq> findByIdAndEmpresaId(Long id, Long empresaId);
}
