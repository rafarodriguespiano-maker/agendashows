package com.example.agendashows.relatorio.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Informa no start da aplicação se o relatório semanal está habilitado e com qual transporte.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RelatorioEmailStatusLogger implements ApplicationRunner {

    private final Environment environment;

    @Value("${app.relatorio-email.remetente:}")
    private String remetente;

    @Value("${app.relatorio-email.cron:0 0 8 * * MON}")
    private String cron;

    @Value("${app.relatorio-email.fuso:America/Sao_Paulo}")
    private String fuso;

    @Override
    public void run(ApplicationArguments args) {
        if (!CondicaoEmail.habilitado(environment)) {
            log.warn("Relatório semanal desabilitado. Configure o envio via Brevo "
                    + "(EMAIL_TRANSPORTE=brevo e BREVO_API_KEY) ou via SMTP "
                    + "(EMAIL_TRANSPORTE=smtp e MAIL_HOST).");
            return;
        }

        log.info("Relatório semanal habilitado: transporte {}, remetente '{}', cron '{}', fuso '{}'.",
                CondicaoEmail.transporte(environment), remetente, cron, fuso);

        if (!StringUtils.hasText(remetente)) {
            log.warn("Relatório semanal sem remetente: defina MAIL_REMETENTE. Sem isso, o envio falhará.");
        }
    }
}
