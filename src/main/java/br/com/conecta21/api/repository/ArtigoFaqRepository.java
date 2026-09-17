package br.com.conecta21.api.repository;

import br.com.conecta21.api.model.ArtigoFaq;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ArtigoFaqRepository extends JpaRepository<ArtigoFaq, Long>, JpaSpecificationExecutor<ArtigoFaq> {

    Optional<ArtigoFaq> findByIdAndEmpresaId(Long id, Long empresaId);

    @Query(
            value = """
                    SELECT a.*
                    FROM artigos_faq a
                    WHERE a.empresa_id = :empresaId
                      AND MATCH(a.titulo, a.conteudo)
                          AGAINST (:busca IN NATURAL LANGUAGE MODE)
                    ORDER BY MATCH(a.titulo, a.conteudo)
                          AGAINST (:busca IN NATURAL LANGUAGE MODE) DESC,
                             a.data_criacao DESC
                    """,
            countQuery = """
                    SELECT COUNT(*)
                    FROM artigos_faq a
                    WHERE a.empresa_id = :empresaId
                      AND MATCH(a.titulo, a.conteudo)
                          AGAINST (:busca IN NATURAL LANGUAGE MODE)
                    """,
            nativeQuery = true)
    Page<ArtigoFaq> pesquisarFullText(
            @Param("empresaId") Long empresaId,
            @Param("busca") String busca,
            Pageable pageable);
}
