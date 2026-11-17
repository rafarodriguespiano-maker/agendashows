package com.example.agendashows.relatorio.email;

import java.util.Locale;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * Regras de habilitação do relatório semanal conforme o transporte configurado.
 */
final class CondicaoEmail {

    static final String TRANSPORTE_PADRAO = "smtp";

    private CondicaoEmail() {
    }

    static String transporte(Environment environment) {
        return environment.getProperty("app.relatorio-email.transporte", TRANSPORTE_PADRAO)
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    static boolean smtpConfigurado(Environment environment) {
        return "smtp".equals(transporte(environment))
                && StringUtils.hasText(environment.getProperty("spring.mail.host"));
    }

    static boolean brevoConfigurado(Environment environment) {
        return "brevo".equals(transporte(environment))
                && StringUtils.hasText(environment.getProperty("app.relatorio-email.brevo.api-key"));
    }

    static boolean habilitado(Environment environment) {
        return smtpConfigurado(environment) || brevoConfigurado(environment);
    }
}
