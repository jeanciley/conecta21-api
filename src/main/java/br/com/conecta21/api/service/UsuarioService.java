package br.com.conecta21.api.service;

import br.com.conecta21.api.dto.UsuarioCadastroDTO;
import br.com.conecta21.api.dto.UsuarioRespostaDTO;
import br.com.conecta21.api.dto.UsuarioPerfilRespostaDTO;
import br.com.conecta21.api.dto.UsuarioPerfilAtualizacaoDTO;
import br.com.conecta21.api.dto.UsuarioTrocaSenhaDTO;
import br.com.conecta21.api.dto.UsuarioListaDTO;
import jakarta.persistence.EntityNotFoundException;
import br.com.conecta21.api.model.Usuario;
import br.com.conecta21.api.model.PerfilUsuario;
import br.com.conecta21.api.security.TenantContext;
import br.com.conecta21.api.repository.UsuarioRepository;
import br.com.conecta21.api.repository.PerfilCustomizadoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UsuarioService {

    private static final long TAMANHO_MAX_AVATAR = 5L * 1024L * 1024L;
    private static final Set<String> EXTENSOES_AVATAR = Set.of("png", "jpg", "jpeg", "webp");
    private static final Set<String> MIMES_AVATAR = Set.of("image/png", "image/jpeg", "image/webp");

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TenantContext tenantContext;

    @Autowired
    private ArquivoStorageService storageService;

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
        alvo.setExcluido(true);
        alvo.setExcluidoEm(LocalDateTime.now());
    }

    private UsuarioRespostaDTO toResposta(Usuario usuario) {
        return new UsuarioRespostaDTO(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getPerfil(),
                usuario.getPerfilCustomizado() != null ? usuario.getPerfilCustomizado().getNome() : usuario.getPerfil().name(),
                usuario.getPerfilCustomizado() != null ? usuario.getPerfilCustomizado().getId() : null,
                usuario.isAtivo(), usuario.getExcluidoEm() != null,
                java.util.Set.of("CHAMADOS_INTERNOS", "GERENCIAR_CHAMADOS", "GERENCIAR_CATEGORIAS", "GERENCIAR_PRIORIDADES", "GERENCIAR_USUARIOS", "GERENCIAR_FAQ", "GERAR_RELATORIOS").stream().filter(usuario::temPermissao).collect(java.util.stream.Collectors.toSet()));
    }

    @Transactional(readOnly = true)
    public UsuarioPerfilRespostaDTO meuPerfil() {
        return toPerfil(tenantContext.getUsuarioAutenticado());
    }

    @Transactional
    public UsuarioPerfilRespostaDTO atualizarMeuPerfil(UsuarioPerfilAtualizacaoDTO dto) {
        Usuario usuario = tenantContext.getUsuarioAutenticado();
        String emailNormalizado = dto.email().trim().toLowerCase(Locale.ROOT);

        if (usuarioRepository.existsByEmailAndIdNot(emailNormalizado, usuario.getId())) {
            throw new IllegalStateException("O e-mail informado já está cadastrado.");
        }

        usuario.setNome(dto.nome().trim());
        usuario.setEmail(emailNormalizado);
        return toPerfil(usuarioRepository.save(usuario));
    }

    @Transactional
    public void trocarMinhaSenha(UsuarioTrocaSenhaDTO dto) {
        Usuario usuario = tenantContext.getUsuarioAutenticado();

        if (!passwordEncoder.matches(dto.senhaAtual(), usuario.getSenha())) {
            throw new IllegalArgumentException("Senha atual inválida.");
        }
        if (passwordEncoder.matches(dto.novaSenha(), usuario.getSenha())) {
            throw new IllegalArgumentException("A nova senha deve ser diferente da senha atual.");
        }

        usuario.setSenha(passwordEncoder.encode(dto.novaSenha()));
        usuarioRepository.save(usuario);
    }

    @Transactional
    public UsuarioPerfilRespostaDTO atualizarAvatar(MultipartFile arquivo) {
        Usuario usuario = tenantContext.getUsuarioAutenticado();
        validarAvatar(arquivo);

        String avatarAnterior = usuario.getAvatarCaminhoRelativo();
        ArquivoStorageService.ArquivoSalvo salvo = storageService.salvar(
                arquivo,
                Path.of("avatars", usuario.getEmpresa().getId().toString(), usuario.getId().toString()),
                EXTENSOES_AVATAR);

        registrarRemocaoEmRollback(salvo.caminhoRelativo());

        usuario.setAvatarNomeOriginal(salvo.nomeOriginal());
        usuario.setAvatarTipoMime(salvo.tipoMime());
        usuario.setAvatarCaminhoRelativo(salvo.caminhoRelativo());
        usuarioRepository.save(usuario);

        if (avatarAnterior != null && !avatarAnterior.isBlank() && !avatarAnterior.equals(salvo.caminhoRelativo())) {
            registrarRemocaoAposCommit(avatarAnterior);
        }

        return toPerfil(usuario);
    }

    @Transactional(readOnly = true)
    public AvatarDownload carregarMeuAvatar() {
        Usuario usuario = tenantContext.getUsuarioAutenticado();
        if (usuario.getAvatarCaminhoRelativo() == null || usuario.getAvatarCaminhoRelativo().isBlank()) {
            throw new jakarta.persistence.EntityNotFoundException("Avatar não encontrado.");
        }

        try {
            Resource resource = storageService.carregar(usuario.getAvatarCaminhoRelativo());
            return new AvatarDownload(resource, usuario.getAvatarNomeOriginal(), usuario.getAvatarTipoMime());
        } catch (IllegalArgumentException ex) {
            throw new jakarta.persistence.EntityNotFoundException("Avatar não encontrado.");
        }
    }

    @Transactional
    public void removerMeuAvatar() {
        Usuario usuario = tenantContext.getUsuarioAutenticado();
        String caminhoAnterior = usuario.getAvatarCaminhoRelativo();
        if (caminhoAnterior == null || caminhoAnterior.isBlank()) {
            return;
        }

        usuario.setAvatarNomeOriginal(null);
        usuario.setAvatarTipoMime(null);
        usuario.setAvatarCaminhoRelativo(null);
        usuarioRepository.save(usuario);
        registrarRemocaoAposCommit(caminhoAnterior);
    }

    private void validarAvatar(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("O avatar enviado está vazio.");
        }
        if (arquivo.getSize() > TAMANHO_MAX_AVATAR) {
            throw new IllegalArgumentException("O avatar deve possuir no máximo 5 MB.");
        }

        String contentType = arquivo.getContentType();
        if (contentType == null || !MIMES_AVATAR.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Formato de avatar inválido. Use PNG, JPG/JPEG ou WEBP.");
        }

        String nome = arquivo.getOriginalFilename() == null ? "" : arquivo.getOriginalFilename().toLowerCase(Locale.ROOT);
        String extensao = nome.contains(".") ? nome.substring(nome.lastIndexOf('.') + 1) : "";
        if (!EXTENSOES_AVATAR.contains(extensao)) {
            throw new IllegalArgumentException("Formato de avatar inválido. Use PNG, JPG/JPEG ou WEBP.");
        }

        if (!assinaturaCompativel(arquivo, extensao)) {
            throw new IllegalArgumentException("O conteúdo do arquivo não corresponde a uma imagem válida.");
        }
    }

    private boolean assinaturaCompativel(MultipartFile arquivo, String extensao) {
        try (InputStream in = arquivo.getInputStream()) {
            byte[] cabecalho = in.readNBytes(12);
            if ("png".equals(extensao)) {
                return cabecalho.length >= 8
                        && (cabecalho[0] & 0xFF) == 0x89
                        && cabecalho[1] == 0x50
                        && cabecalho[2] == 0x4E
                        && cabecalho[3] == 0x47
                        && cabecalho[4] == 0x0D
                        && cabecalho[5] == 0x0A
                        && cabecalho[6] == 0x1A
                        && cabecalho[7] == 0x0A;
            }
            if ("jpg".equals(extensao) || "jpeg".equals(extensao)) {
                return cabecalho.length >= 3
                        && (cabecalho[0] & 0xFF) == 0xFF
                        && (cabecalho[1] & 0xFF) == 0xD8
                        && (cabecalho[2] & 0xFF) == 0xFF;
            }
            if ("webp".equals(extensao)) {
                return cabecalho.length >= 12
                        && cabecalho[0] == 'R' && cabecalho[1] == 'I'
                        && cabecalho[2] == 'F' && cabecalho[3] == 'F'
                        && cabecalho[8] == 'W' && cabecalho[9] == 'E'
                        && cabecalho[10] == 'B' && cabecalho[11] == 'P';
            }
            return false;
        } catch (IOException ex) {
            throw new IllegalArgumentException("Não foi possível validar o avatar enviado.", ex);
        }
    }

    private void registrarRemocaoEmRollback(String caminhoRelativo) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_COMMITTED) {
                    storageService.removerSilenciosamente(caminhoRelativo);
                }
            }
        });
    }

    private void registrarRemocaoAposCommit(String caminhoRelativo) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            storageService.removerSilenciosamente(caminhoRelativo);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                storageService.removerSilenciosamente(caminhoRelativo);
            }
        });
    }

    private UsuarioPerfilRespostaDTO toPerfil(Usuario usuario) {
        return new UsuarioPerfilRespostaDTO(
                usuario.getId(),
                usuario.getEmpresa().getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getPerfil(),
                usuario.getAvatarCaminhoRelativo() != null && !usuario.getAvatarCaminhoRelativo().isBlank());
    }

    @Transactional(readOnly = true)
    public List<UsuarioListaDTO> listarMembrosDaEmpresa() {
        Usuario adminLogado = tenantContext.getUsuarioAutenticado();
        if (!adminLogado.temPermissao("GERENCIAR_USUARIOS")) {
            throw new org.springframework.security.access.AccessDeniedException("Sem permissÃ£o para consultar usuÃ¡rios.");
        }

        return usuarioRepository.findByEmpresaId(adminLogado.getEmpresa().getId())
                .stream()
                .map(UsuarioListaDTO::new)
                .collect(Collectors.toList());
    }

    public record AvatarDownload(Resource resource, String nomeOriginal, String tipoMime) {
    }
}
