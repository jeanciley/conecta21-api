package br.com.conecta21.api.dto;

import br.com.conecta21.api.model.PerfilUsuario;
import java.util.Set;

public record UsuarioRespostaDTO(Long id, String nome, String email, PerfilUsuario perfil,
                                 String perfilNome, Long perfilCustomizadoId, boolean ativo, boolean excluido,
                                 Set<String> permissoes) {
}
