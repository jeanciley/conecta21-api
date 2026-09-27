package br.com.conecta21.api.repository;

import br.com.conecta21.api.model.ArtigoFaq;
import org.springframework.data.jpa.domain.Specification;

public final class ArtigoFaqSpecs {

    private ArtigoFaqSpecs() {
    }

    public static Specification<ArtigoFaq> doTenant(Long empresaId) {
        return (root, query, cb) -> cb.equal(root.get("empresa").get("id"), empresaId);
    }
}
