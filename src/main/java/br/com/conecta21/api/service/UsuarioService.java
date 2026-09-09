package br.com.conecta21.api.service;

import br.com.conecta21.api.dto.UsuarioCadastroDTO;
import br.com.conecta21.api.model.Usuario;
import br.com.conecta21.api.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Usuario cadastrarMembro(UsuarioCadastroDTO dto) {

        Usuario adminLogado = (Usuario) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Usuario novoUsuario = new Usuario();
        novoUsuario.setNome(dto.nome());
        novoUsuario.setEmail(dto.email());
        novoUsuario.setSenha(passwordEncoder.encode(dto.senha()));

        novoUsuario.setPerfil(dto.perfil());

        novoUsuario.setEmpresa(adminLogado.getEmpresa());

        return usuarioRepository.save(novoUsuario);
    }
}
