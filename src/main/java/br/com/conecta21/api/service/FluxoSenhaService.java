package br.com.conecta21.api.service;

import br.com.conecta21.api.dto.AtivacaoDTO;
import br.com.conecta21.api.model.TokenUsuario;
import br.com.conecta21.api.model.Usuario;
import br.com.conecta21.api.repository.TokenUsuarioRepository;
import br.com.conecta21.api.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class FluxoSenhaService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final TokenUsuarioRepository tokens; private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder; private final EmailService email;
    public FluxoSenhaService(TokenUsuarioRepository tokens, UsuarioRepository usuarios, PasswordEncoder encoder, EmailService email) { this.tokens=tokens; this.usuarios=usuarios; this.encoder=encoder; this.email=email; }

    @Transactional
    public void enviarAtivacao(Usuario usuario) { emitir(usuario, "ATIVACAO", true); }
    @Transactional
    public void solicitarRedefinicao(String emailInformado) {
        usuarios.findByEmail(emailInformado.trim().toLowerCase()).filter(Usuario::isAtivo)
                .ifPresent(usuario -> emitir(usuario, "REDEFINICAO", false));
    }
    @Transactional
    public void ativar(AtivacaoDTO dto) { consumir(dto, "ATIVACAO", true); }
    @Transactional
    public void redefinir(AtivacaoDTO dto) { consumir(dto, "REDEFINICAO", false); }

    private void emitir(Usuario usuario, String finalidade, boolean ativacao) {
        byte[] bytes = new byte[32]; RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        TokenUsuario token = new TokenUsuario(); token.setUsuario(usuario); token.setTokenHash(hash(raw));
        token.setFinalidade(finalidade); token.setExpiraEm(LocalDateTime.now().plusMinutes(30)); tokens.save(token);
        boolean enviado = email.enviarLink(usuario.getEmail(), usuario.getNome(), raw, ativacao);
        if (ativacao && !enviado) throw new IllegalStateException("Não foi possível enviar o e-mail de ativação. Confira a configuração SMTP e tente novamente.");
    }
    private void consumir(AtivacaoDTO dto, String finalidade, boolean ativacao) {
        TokenUsuario token = tokens.findByTokenHashAndFinalidadeAndUsadoEmIsNull(hash(dto.token()), finalidade)
                .filter(t -> t.getExpiraEm().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new IllegalArgumentException("Link inválido ou expirado. Solicite um novo e-mail."));
        Usuario usuario = token.getUsuario(); usuario.setSenha(encoder.encode(dto.senha()));
        if (ativacao) usuario.setAtivo(true);
        token.setUsadoEm(LocalDateTime.now());
    }
    private String hash(String valor) {
        try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8))); }
        catch (Exception ex) { throw new IllegalStateException("Não foi possível processar o token.", ex); }
    }
}
