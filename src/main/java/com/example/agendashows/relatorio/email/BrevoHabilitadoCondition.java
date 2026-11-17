package com.example.agendashows.relatorio.email;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Ativa o transporte Brevo quando {@code EMAIL_TRANSPORTE=brevo} e há {@code BREVO_API_KEY}.
 */
public class BrevoHabilitadoCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return CondicaoEmail.brevoConfigurado(context.getEnvironment());
    }
}
