package br.com.conecta21.api.repository;

import br.com.conecta21.api.model.ArtigoFaq;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ArtigoFaqRepository extends JpaRepository<ArtigoFaq, Long> {

    List<ArtigoFaq> findAllByEmpresaId(Long empresaId);

    Optional<ArtigoFaq> findByIdAndEmpresaId(Long id, Long empresaId);

    @Query("SELECT a FROM ArtigoFaq a WHERE a.empresa.id = :empresaId AND " +
            "(LOWER(a.titulo) LIKE LOWER(CONCAT('%', :termo, '%')) OR " +
            "LOWER(a.conteudo) LIKE LOWER(CONCAT('%', :termo, '%')))")
    List<ArtigoFaq> buscarPorTermo(@Param("empresaId") Long empresaId, @Param("termo") String termo);

    boolean existsByTituloIgnoreCaseAndEmpresaId(String titulo, Long empresaId);
}
