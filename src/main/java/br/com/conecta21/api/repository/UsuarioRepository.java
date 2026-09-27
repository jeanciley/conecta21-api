package br.com.conecta21.api.repository;

import br.com.conecta21.api.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    Optional<Usuario> findByEmailIgnoreCase(String email);

    List<Usuario> findAllByEmpresaId(Long empresaId);

    long countByPerfilCustomizadoId(Long perfilCustomizadoId);

    boolean existsByEmailAndIdNot(String email, Long id);

    List<Usuario> findByEmpresaId(Long empresaId);
}
