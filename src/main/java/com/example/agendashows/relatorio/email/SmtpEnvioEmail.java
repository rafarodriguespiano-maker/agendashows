package com.example.agendashows.relatorio.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Transporte via SMTP. Funciona em VPS/desenvolvimento; a Railway bloqueia SMTP nos planos
 * Free, Trial e Hobby (use o transporte Brevo nesses casos).
 */
@Component
@Conditional(SmtpHabilitadoCondition.class)
public class SmtpEnvioEmail implements EnvioEmail {

    private final JavaMailSender mailSender;
    private final String remetente;
    private final String nomeRemetente;

    public SmtpEnvioEmail(JavaMailSender mailSender,
                          @Value("${app.relatorio-email.remetente:}") String remetente,
                          @Value("${app.relatorio-email.nome-remetente:}") String nomeRemetente) {
        this.mailSender = mailSender;
        this.remetente = remetente;
        this.nomeRemetente = nomeRemetente;
    }

    @Override
    public void enviar(EmailMensagem mensagem) {
        try {
            MimeMessage email = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(email, true, "UTF-8");
            if (StringUtils.hasText(nomeRemetente)) {
                helper.setFrom(remetente, nomeRemetente);
            } else {
                helper.setFrom(remetente);
            }
            helper.setTo(mensagem.destinatario());
            helper.setSubject(mensagem.assunto());
            helper.setText(mensagem.texto(), mensagem.html());
            mailSender.send(email);
        } catch (MessagingException | MailException | UnsupportedEncodingException ex) {
            throw new EnvioEmailException("Falha no envio via SMTP para " + mensagem.destinatario(), ex);
        }
    }
}
