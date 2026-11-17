package com.example.agendashows.relatorio.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;

class SmtpEnvioEmailTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);

    @Test
    void enviaMensagemComRemetenteEDestinatario() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        SmtpEnvioEmail envioEmail = new SmtpEnvioEmail(mailSender, "nao-responda@exemplo.com", "Agenda de Shows");

        envioEmail.enviar(new EmailMensagem("musico@exemplo.com", "Shows da semana", "<html>ok</html>", "ok"));

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage mensagem = captor.getValue();
        assertThat(mensagem.getAllRecipients()).extracting(Object::toString)
                .containsExactly("musico@exemplo.com");
        assertThat(mensagem.getSubject()).isEqualTo("Shows da semana");
    }
}
