package com.example.agendashows.relatorio;

import com.example.agendashows.relatorio.email.RelatorioEmailHabilitadoCondition;
import com.example.agendashows.relatorio.service.RelatorioEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Conditional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Dispara o relatório semanal toda segunda-feira e quinta-feira às 8h (fuso configurável).
 * Uma falha no envio é registrada em log sem interromper a aplicação.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Conditional(RelatorioEmailHabilitadoCondition.class)
public class RelatorioSemanalScheduler {

    private final RelatorioEmailService relatorioEmailService;

    @Scheduled(cron = "${app.relatorio-email.cron}",
            zone = "${app.relatorio-email.fuso:America/Sao_Paulo}")
    public void enviarRelatorioSemanal() {
        log.info("Iniciando envio do relatório semanal de shows.");
        try {
            relatorioEmailService.enviar();
        } catch (RuntimeException ex) {
            log.error("Erro inesperado ao processar o relatório semanal de shows.", ex);
        }
    }
}
