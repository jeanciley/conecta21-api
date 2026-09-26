package br.com.conecta21.api.repository;

import br.com.conecta21.api.model.AnexoInteracao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AnexoInteracaoRepository extends JpaRepository<AnexoInteracao, Long> {
    Optional<AnexoInteracao> findByIdAndInteracaoIdAndInteracaoChamadoIdAndInteracaoChamadoEmpresaId(
            Long id, Long interacaoId, Long chamadoId, Long empresaId);
}
