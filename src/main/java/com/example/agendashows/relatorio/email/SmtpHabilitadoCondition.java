package com.example.agendashows.relatorio.email;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Ativa o transporte SMTP quando {@code EMAIL_TRANSPORTE=smtp} (padrão) e há {@code MAIL_HOST}.
 */
public class SmtpHabilitadoCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return CondicaoEmail.smtpConfigurado(context.getEnvironment());
    }
}
