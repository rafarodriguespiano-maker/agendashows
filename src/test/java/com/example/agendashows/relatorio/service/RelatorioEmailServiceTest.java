package com.example.agendashows.relatorio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.agendashows.relatorio.dto.RelatorioSemanal;
import com.example.agendashows.relatorio.dto.ShowRelatorio;
import com.example.agendashows.user.entity.AppUser;
import com.example.agendashows.user.repository.AppUserRepository;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@ExtendWith(MockitoExtension.class)
class RelatorioEmailServiceTest {

    @Mock
    private RelatorioSemanalService relatorioSemanalService;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    private RelatorioEmailService relatorioEmailService;

    @BeforeEach
    void preparar() {
        relatorioEmailService = new RelatorioEmailService(
                relatorioSemanalService, appUserRepository, mailSender, templateEngine,
                "nao-responda@agendashows.com", "America/Sao_Paulo");
    }

    @Test
    void enviaRelatorioParaUsuarioPrincipal() throws Exception {
        AppUser usuario = usuario("musico", "musico@exemplo.com");
        when(appUserRepository.findFirstByAtivoTrueAndEmailIsNotNullOrderByIdAsc())
                .thenReturn(Optional.of(usuario));
        when(relatorioSemanalService.daSemana(any())).thenReturn(relatorioComShows());
        when(mailSender.createMimeMessage()).thenReturn(novaMensagem());
        when(templateEngine.process(eq("email/relatorio-semanal"), any(Context.class)))
                .thenReturn("<html>relatório</html>");

        relatorioEmailService.enviar();

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage mensagem = captor.getValue();
        assertThat(mensagem.getAllRecipients()).extracting(Object::toString)
                .containsExactly("musico@exemplo.com");
        assertThat(mensagem.getSubject()).isEqualTo("Shows de 09/03 a 15/03");
    }

    @Test
    void naoEnviaSemUsuarioComEmail() {
        when(appUserRepository.findFirstByAtivoTrueAndEmailIsNotNullOrderByIdAsc())
                .thenReturn(Optional.empty());

        relatorioEmailService.enviar();

        verifyNoInteractions(mailSender);
    }

    @Test
    void naoEnviaSemRemetenteConfigurado() {
        relatorioEmailService = new RelatorioEmailService(
                relatorioSemanalService, appUserRepository, mailSender, templateEngine,
                "", "America/Sao_Paulo");
        when(appUserRepository.findFirstByAtivoTrueAndEmailIsNotNullOrderByIdAsc())
                .thenReturn(Optional.of(usuario("musico", "musico@exemplo.com")));

        relatorioEmailService.enviar();

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    private static AppUser usuario(String nome, String email) {
        AppUser usuario = new AppUser();
        usuario.setNomeUsuario(nome);
        usuario.setEmail(email);
        return usuario;
    }

    private static RelatorioSemanal relatorioComShows() {
        return new RelatorioSemanal(
                LocalDate.of(2026, 3, 9),
                LocalDate.of(2026, 3, 15),
                "09/03/2026 a 15/03/2026",
                List.of(new ShowRelatorio("seg, 09/03", "20:00", "Banda", "Bar",
                        "R$ 1.000,00", "Pago", "—")),
                "R$ 1.000,00",
                1,
                0);
    }

    private static MimeMessage novaMensagem() {
        return new MimeMessage(Session.getInstance(new Properties()));
    }
}
