package com.example.agendashows.relatorio.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.agendashows.relatorio.dto.RelatorioSemanal;
import com.example.agendashows.relatorio.dto.ShowRelatorio;
import com.example.agendashows.relatorio.email.EmailMensagem;
import com.example.agendashows.relatorio.email.EnvioEmail;
import com.example.agendashows.relatorio.email.EnvioEmailException;
import com.example.agendashows.user.entity.AppUser;
import com.example.agendashows.user.repository.AppUserRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@ExtendWith(MockitoExtension.class)
class RelatorioEmailServiceTest {

    @Mock
    private RelatorioSemanalService relatorioSemanalService;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private EnvioEmail envioEmail;

    @Mock
    private TemplateEngine templateEngine;

    private RelatorioEmailService relatorioEmailService;

    @BeforeEach
    void preparar() {
        relatorioEmailService = new RelatorioEmailService(
                relatorioSemanalService, appUserRepository, envioEmail, templateEngine,
                "nao-responda@agendashows.com", "America/Sao_Paulo");
    }

    @Test
    void enviaRelatorioParaUsuarioPrincipal() {
        when(appUserRepository.findFirstByAtivoTrueAndEmailIsNotNullOrderByIdAsc())
                .thenReturn(Optional.of(usuario("musico", "musico@exemplo.com")));
        when(relatorioSemanalService.daSemana(any())).thenReturn(relatorioComShows());
        when(templateEngine.process(eq("email/relatorio-semanal"), any(Context.class)))
                .thenReturn("<html>relatório</html>");

        relatorioEmailService.enviar();

        ArgumentCaptor<EmailMensagem> captor = ArgumentCaptor.forClass(EmailMensagem.class);
        verify(envioEmail).enviar(captor.capture());
        EmailMensagem mensagem = captor.getValue();
        assertThat(mensagem.destinatario()).isEqualTo("musico@exemplo.com");
        assertThat(mensagem.assunto()).isEqualTo("Shows de 09/03 a 15/03");
        assertThat(mensagem.html()).isEqualTo("<html>relatório</html>");
        assertThat(mensagem.texto()).contains("Banda").contains("Total: 1 show(s)");
    }

    @Test
    void naoEnviaSemUsuarioComEmail() {
        when(appUserRepository.findFirstByAtivoTrueAndEmailIsNotNullOrderByIdAsc())
                .thenReturn(Optional.empty());

        relatorioEmailService.enviar();

        verifyNoInteractions(envioEmail);
    }

    @Test
    void naoEnviaSemRemetenteConfigurado() {
        relatorioEmailService = new RelatorioEmailService(
                relatorioSemanalService, appUserRepository, envioEmail, templateEngine,
                "", "America/Sao_Paulo");
        when(appUserRepository.findFirstByAtivoTrueAndEmailIsNotNullOrderByIdAsc())
                .thenReturn(Optional.of(usuario("musico", "musico@exemplo.com")));

        relatorioEmailService.enviar();

        verify(envioEmail, never()).enviar(any());
    }

    @Test
    void falhaDoTransporteNaoPropagaExcecao() {
        when(appUserRepository.findFirstByAtivoTrueAndEmailIsNotNullOrderByIdAsc())
                .thenReturn(Optional.of(usuario("musico", "musico@exemplo.com")));
        when(relatorioSemanalService.daSemana(any())).thenReturn(relatorioComShows());
        when(templateEngine.process(eq("email/relatorio-semanal"), any(Context.class)))
                .thenReturn("<html>ok</html>");
        doThrow(new EnvioEmailException("SMTP fora do ar")).when(envioEmail).enviar(any());

        assertThatCode(() -> relatorioEmailService.enviar()).doesNotThrowAnyException();
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
}
