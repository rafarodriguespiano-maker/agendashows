package com.example.agendashows.relatorio;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.agendashows.relatorio.email.BrevoEnvioEmail;
import com.example.agendashows.relatorio.email.EnvioEmail;
import com.example.agendashows.relatorio.service.RelatorioEmailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Com EMAIL_TRANSPORTE=brevo e API key, o transporte HTTPS é usado (Railway Free/Hobby).
 */
@SpringBootTest(properties = {
        "app.relatorio-email.transporte=brevo",
        "app.relatorio-email.brevo.api-key=chave-teste",
        "app.relatorio-email.remetente=teste@exemplo.com"
})
@ActiveProfiles("test")
class RelatorioSemanalBrevoContextTest {

    @Autowired
    private EnvioEmail envioEmail;

    @Autowired
    private RelatorioEmailService relatorioEmailService;

    @Autowired
    private RelatorioSemanalScheduler relatorioSemanalScheduler;

    @Test
    void usaTransporteBrevoEAtivaRelatorio() {
        assertThat(envioEmail).isInstanceOf(BrevoEnvioEmail.class);
        assertThat(relatorioEmailService).isNotNull();
        assertThat(relatorioSemanalScheduler).isNotNull();
    }
}
