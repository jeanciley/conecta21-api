package br.com.conecta21.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class NotificacaoEmailService {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoEmailService.class);

    private final JavaMailSender mailSender;
    private final String remetente;

    public NotificacaoEmailService(
            JavaMailSender mailSender,
            @Value("${app.mail.from:no-reply@conecta21.local}") String remetente) {
        this.mailSender = mailSender;
        this.remetente = remetente;
    }

    @Async("notificacaoExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void enviarAlteracaoStatus(ChamadoStatusAlteradoEvent evento) {
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(remetente);
        mensagem.setTo(evento.emailSolicitante());
        mensagem.setSubject("Chamado #" + evento.chamadoId() + " teve o status alterado");
        mensagem.setText("""
                Olá, %s!

                O status do chamado #%d - %s foi alterado de %s para %s.

                Esta é uma mensagem automática do Conecta21.
                """.formatted(
                evento.nomeSolicitante(),
                evento.chamadoId(),
                evento.titulo(),
                evento.statusAnterior().name(),
                evento.statusNovo().name()));

        try {
            mailSender.send(mensagem);
            log.info("Notificação do chamado {} enviada para {}", evento.chamadoId(), evento.emailSolicitante());
        } catch (MailException ex) {
            // O envio é assíncrono e não deve desfazer a alteração de status já confirmada no banco.
            log.error("Falha ao enviar notificação do chamado {} para {}",
                    evento.chamadoId(), evento.emailSolicitante(), ex);
        }
    }
}
