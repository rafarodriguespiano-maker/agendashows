package com.example.agendashows.relatorio;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.agendashows.relatorio.dto.RelatorioSemanal;
import com.example.agendashows.relatorio.dto.ShowRelatorio;
import com.example.agendashows.relatorio.service.RelatorioEmailService;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Com o SMTP configurado, os beans do relatório são criados e o template do e-mail é renderizado.
 */
@SpringBootTest(properties = {
        "spring.mail.host=localhost",
        "spring.mail.port=1025",
        "app.relatorio-email.remetente=teste@exemplo.com"
})
@ActiveProfiles("test")
class RelatorioSemanalContextTest {

    @Autowired
    private RelatorioEmailService relatorioEmailService;

    @Autowired
    private RelatorioSemanalScheduler relatorioSemanalScheduler;

    @Autowired
    private TemplateEngine templateEngine;

    @Test
    void criaBeansQuandoSmtpEstaConfigurado() {
        assertThat(relatorioEmailService).isNotNull();
        assertThat(relatorioSemanalScheduler).isNotNull();
    }

    @Test
    void renderizaTemplateDoRelatorio() {
        RelatorioSemanal relatorio = new RelatorioSemanal(
                LocalDate.of(2026, 3, 9),
                LocalDate.of(2026, 3, 15),
                "09/03/2026 a 15/03/2026",
                List.of(new ShowRelatorio("seg, 09/03", "20:00", "Banda Teste", "Bar do Zé",
                        "R$ 1.000,00", "Pago", "Levar equipamento")),
                "R$ 1.000,00",
                1,
                0);

        Context contexto = new Context(Locale.forLanguageTag("pt-BR"));
        contexto.setVariable("relatorio", relatorio);
        contexto.setVariable("nomeUsuario", "musico");

        String html = templateEngine.process("email/relatorio-semanal", contexto);

        assertThat(html)
                .contains("Shows da semana")
                .contains("Banda Teste")
                .contains("Bar do Zé")
                .contains("Levar equipamento")
                .contains("09/03/2026 a 15/03/2026")
                .doesNotContain("Nenhum show agendado");
    }
}
