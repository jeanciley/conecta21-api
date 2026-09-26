package br.com.conecta21.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private final JavaMailSender sender;
    @Value("${app.frontend-url:http://localhost:5500}") private String frontendUrl;
    @Value("${spring.mail.username:}") private String from;
    public EmailService(JavaMailSender sender) { this.sender = sender; }
    public boolean enviarLink(String destino, String nome, String token, boolean ativacao) {
        String link = frontendUrl.replaceAll("/$", "") + (ativacao ? "/ativar-conta.html" : "/redefinir-senha.html") + "?token=" + token;
        SimpleMailMessage mensagem = new SimpleMailMessage();
        if (!from.isBlank()) mensagem.setFrom(from);
        mensagem.setTo(destino);
        mensagem.setSubject(ativacao ? "Ative seu acesso ao Conecta21" : "Redefinição de senha do Conecta21");
        mensagem.setText("Olá, " + nome + ".\n\nUse o link abaixo para " + (ativacao ? "definir sua senha e ativar sua conta" : "definir uma nova senha") + ":\n" + link + "\n\nO link expira em 30 minutos. Se você não solicitou esta mensagem, ignore-a.");
        try { sender.send(mensagem); return true; }
        catch (MailException ex) { log.error("Falha ao enviar e-mail de autenticação para {}", destino, ex); return false; }
    }
}
