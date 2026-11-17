package com.example.agendashows.relatorio.email;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Registra o serviço/agendador do relatório quando qualquer transporte está configurado.
 */
public class RelatorioEmailHabilitadoCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return CondicaoEmail.habilitado(context.getEnvironment());
    }
}
