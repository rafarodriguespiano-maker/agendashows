package com.example.agendashows.relatorio;

import com.example.agendashows.relatorio.service.RelatorioEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Dispara o relatório semanal toda segunda-feira às 8h (fuso configurável).
 * Uma falha no envio é registrada em log sem interromper a aplicação.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "spring.mail", name = "host")
public class RelatorioSemanalScheduler {

    private final RelatorioEmailService relatorioEmailService;

    @Value("${spring.mail.host}")
    private String host;

    @Value("${spring.mail.port:587}")
    private int porta;

    @Value("${app.relatorio-email.remetente:}")
    private String remetente;

    @Value("${app.relatorio-email.cron:0 0 8 * * MON}")
    private String cron;

    @Value("${app.relatorio-email.fuso:America/Sao_Paulo}")
    private String fuso;

    @EventListener(ApplicationReadyEvent.class)
    public void registrarConfiguracao() {
        log.info("Relatório semanal habilitado: SMTP {}:{}, cron '{}', fuso '{}'.",
                host, porta, cron, fuso);
        if (!StringUtils.hasText(remetente)) {
            log.warn("Relatório semanal sem remetente: defina MAIL_REMETENTE (ou MAIL_USUARIO). "
                    + "Sem isso, o envio falhará.");
        }
    }

    @Scheduled(cron = "${app.relatorio-email.cron:0 0 8 * * MON}",
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
