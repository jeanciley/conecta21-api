package br.com.conecta21.api.service;

import br.com.conecta21.api.dto.UsuarioCadastroDTO;
import br.com.conecta21.api.dto.UsuarioRespostaDTO;
import br.com.conecta21.api.model.Usuario;
import br.com.conecta21.api.model.PerfilUsuario;
import br.com.conecta21.api.security.TenantContext;
import br.com.conecta21.api.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TenantContext tenantContext;

    @Autowired
    private FluxoSenhaService fluxoSenhaService;

    @Transactional
    public Usuario cadastrarMembro(UsuarioCadastroDTO dto) {

        Usuario adminLogado = tenantContext.getUsuarioAutenticado();
        if (adminLogado.getPerfil() != PerfilUsuario.ADMIN || dto.perfil() == PerfilUsuario.ADMIN) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Apenas administradores podem cadastrar membros técnicos ou usuários.");
        }

        Usuario novoUsuario = new Usuario();
        novoUsuario.setNome(dto.nome());
        novoUsuario.setEmail(dto.email());
        novoUsuario.setSenha(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));

        novoUsuario.setPerfil(dto.perfil());

        novoUsuario.setEmpresa(adminLogado.getEmpresa());
        novoUsuario.setAtivo(false);

        Usuario salvo = usuarioRepository.save(novoUsuario);
        fluxoSenhaService.enviarAtivacao(salvo);
        return salvo;
    }

    @Transactional(readOnly = true)
    public List<UsuarioRespostaDTO> listarMembros() {
        Long empresaId = tenantContext.getEmpresaIdAutenticada();
        return usuarioRepository.findAllByEmpresaId(empresaId).stream()
                .map(this::toResposta)
                .toList();
    }

    @Transactional(readOnly = true)
    public UsuarioRespostaDTO obterPerfilLogado() {
        return toResposta(tenantContext.getUsuarioAutenticado());
    }

    private UsuarioRespostaDTO toResposta(Usuario usuario) {
        return new UsuarioRespostaDTO(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getPerfil(), usuario.isAtivo());
    }
}
