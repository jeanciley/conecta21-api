package br.com.conecta21.api.service;

import br.com.conecta21.api.dto.UsuarioCadastroDTO;
import br.com.conecta21.api.dto.UsuarioRespostaDTO;
import br.com.conecta21.api.model.Usuario;
import br.com.conecta21.api.model.PerfilUsuario;
import br.com.conecta21.api.security.TenantContext;
import br.com.conecta21.api.repository.UsuarioRepository;
import br.com.conecta21.api.repository.PerfilCustomizadoRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.LocalDateTime;

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

    @Autowired
    private PerfilCustomizadoRepository perfilRepository;

    @Transactional
    public Usuario cadastrarMembro(UsuarioCadastroDTO dto) {

        Usuario adminLogado = tenantContext.getUsuarioAutenticado();
        if (!adminLogado.temPermissao("GERENCIAR_USUARIOS") || dto.perfil() == PerfilUsuario.ADMIN) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Apenas administradores podem cadastrar membros técnicos ou usuários.");
        }

        Usuario novoUsuario = new Usuario();
        novoUsuario.setNome(dto.nome());
        novoUsuario.setEmail(dto.email());
        novoUsuario.setSenha(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));

        novoUsuario.setPerfil(dto.perfil());

        if (dto.perfilCustomizadoId() != null) {
            var perfil = perfilRepository.findByIdAndEmpresaId(dto.perfilCustomizadoId(), adminLogado.getEmpresa().getId())
                    .filter(p -> p.isAtivo()).orElseThrow(() -> new IllegalArgumentException("Perfil personalizado não encontrado ou inativo."));
            novoUsuario.setPerfil(PerfilUsuario.USUARIO);
            novoUsuario.setPerfilCustomizado(perfil);
        }

        novoUsuario.setEmpresa(adminLogado.getEmpresa());
        novoUsuario.setAtivo(false);

        Usuario salvo = usuarioRepository.save(novoUsuario);
        fluxoSenhaService.enviarAtivacao(salvo);
        return salvo;
    }

    @Transactional(readOnly = true)
    public List<UsuarioRespostaDTO> listarMembros() {
        if (!tenantContext.getUsuarioAutenticado().temPermissao("GERENCIAR_USUARIOS")) throw new org.springframework.security.access.AccessDeniedException("Sem permissÃ£o para consultar usuÃ¡rios.");
        Long empresaId = tenantContext.getEmpresaIdAutenticada();
        return usuarioRepository.findAllByEmpresaId(empresaId).stream()
                .map(this::toResposta)
                .toList();
    }

    @Transactional(readOnly = true)
    public UsuarioRespostaDTO obterPerfilLogado() {
        return toResposta(tenantContext.getUsuarioAutenticado());
    }

    @Transactional
    public void excluir(Long id) {
        Usuario ator = tenantContext.getUsuarioAutenticado();
        if (!ator.temPermissao("GERENCIAR_USUARIOS")) throw new org.springframework.security.access.AccessDeniedException("Sem permissão para excluir usuários.");
        if (ator.getId().equals(id)) throw new IllegalArgumentException("Não é permitido excluir o próprio usuário.");
        Usuario alvo = usuarioRepository.findById(id).filter(u -> u.getEmpresa().getId().equals(ator.getEmpresa().getId()))
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado."));
        alvo.setAtivo(false);
        alvo.setExcluidoEm(LocalDateTime.now());
    }

    private UsuarioRespostaDTO toResposta(Usuario usuario) {
        return new UsuarioRespostaDTO(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getPerfil(),
                usuario.getPerfilCustomizado() != null ? usuario.getPerfilCustomizado().getNome() : usuario.getPerfil().name(),
                usuario.getPerfilCustomizado() != null ? usuario.getPerfilCustomizado().getId() : null,
                usuario.isAtivo(), usuario.getExcluidoEm() != null,
                java.util.Set.of("CHAMADOS_INTERNOS", "GERENCIAR_CHAMADOS", "GERENCIAR_CATEGORIAS", "GERENCIAR_PRIORIDADES", "GERENCIAR_USUARIOS", "GERENCIAR_FAQ", "GERAR_RELATORIOS").stream().filter(usuario::temPermissao).collect(java.util.stream.Collectors.toSet()));
    }
}
