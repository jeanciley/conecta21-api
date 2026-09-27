package br.com.conecta21.api.repository;

import br.com.conecta21.api.model.TokenUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TokenUsuarioRepository extends JpaRepository<TokenUsuario, Long> {
    Optional<TokenUsuario> findByTokenHashAndFinalidadeAndUsadoEmIsNull(String tokenHash, String finalidade);
}
