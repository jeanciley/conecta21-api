package br.com.conecta21.api.repository;

import br.com.conecta21.api.model.Chamado;
import br.com.conecta21.api.model.StatusChamado;
import br.com.conecta21.api.model.TipoChamado;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Filtros dinâmicos da listagem de chamados (Backend C — Evolução Operacional).
 *
 * <p>A trava SaaS ({@code empresa_id}) é sempre a cláusula WHERE principal:
 * todo Specification começa por {@code empresa.id = :empresaId}, e os filtros
 * opcionais (status, técnico, intervalo de dataAbertura) entram apenas como
 * {@code AND} adicionais.
 */
public final class ChamadoSpecs {

    private ChamadoSpecs() {
    }

    public static Specification<Chamado> noTenantComFiltros(
            Long empresaId, StatusChamado status, Long tecnicoId,
            LocalDateTime dataInicio, LocalDateTime dataFim,
            List<TipoChamado> tipos) { // Novo parâmetro

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("empresa").get("id"), empresaId));

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (tecnicoId != null) {
                predicates.add(cb.equal(root.get("tecnico").get("id"), tecnicoId));
            }
            if (dataInicio != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dataAbertura"), dataInicio));
            }
            if (dataFim != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dataAbertura"), dataFim));
            }

            // NOVA LÓGICA: Se o front-end enviou os tipos, filtra por eles
            if (tipos != null && !tipos.isEmpty()) {
                predicates.add(root.get("tipo").in(tipos));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
