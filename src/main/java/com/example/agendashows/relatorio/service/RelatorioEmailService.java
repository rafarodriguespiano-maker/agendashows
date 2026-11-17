package com.example.agendashows.relatorio.service;

import com.example.agendashows.relatorio.dto.RelatorioSemanal;
import com.example.agendashows.relatorio.dto.ShowRelatorio;
import com.example.agendashows.relatorio.email.EmailMensagem;
import com.example.agendashows.relatorio.email.EnvioEmail;
import com.example.agendashows.relatorio.email.EnvioEmailException;
import com.example.agendashows.relatorio.email.RelatorioEmailHabilitadoCondition;
import com.example.agendashows.user.entity.AppUser;
import com.example.agendashows.user.repository.AppUserRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Monta e envia o relatório semanal para o usuário principal (primeiro usuário ativo com e-mail).
 * O transporte (SMTP ou Brevo) é escolhido por {@code EMAIL_TRANSPORTE}.
 */
@Slf4j
@Service
@Conditional(RelatorioEmailHabilitadoCondition.class)
public class RelatorioEmailService {

    private static final String TEMPLATE = "email/relatorio-semanal";
    private static final DateTimeFormatter DIA_MES = DateTimeFormatter.ofPattern("dd/MM", Locale.forLanguageTag("pt-BR"));

    private final RelatorioSemanalService relatorioSemanalService;
    private final AppUserRepository appUserRepository;
    private final EnvioEmail envioEmail;
    private final TemplateEngine templateEngine;
    private final String remetente;
    private final String fuso;

    public RelatorioEmailService(RelatorioSemanalService relatorioSemanalService,
                                 AppUserRepository appUserRepository,
                                 EnvioEmail envioEmail,
                                 TemplateEngine templateEngine,
                                 @Value("${app.relatorio-email.remetente:}") String remetente,
                                 @Value("${app.relatorio-email.fuso:America/Sao_Paulo}") String fuso) {
        this.relatorioSemanalService = relatorioSemanalService;
        this.appUserRepository = appUserRepository;
        this.envioEmail = envioEmail;
        this.templateEngine = templateEngine;
        this.remetente = remetente;
        this.fuso = fuso;
    }

    public void enviar() {
        Optional<AppUser> destinatario = appUserRepository.findFirstByAtivoTrueAndEmailIsNotNullOrderByIdAsc();
        if (destinatario.isEmpty() || !StringUtils.hasText(destinatario.get().getEmail())) {
            log.warn("Relatório semanal não enviado: nenhum usuário ativo possui e-mail cadastrado.");
            return;
        }
        if (!StringUtils.hasText(remetente)) {
            log.error("Relatório semanal não enviado: defina MAIL_REMETENTE ou MAIL_USUARIO.");
            return;
        }

        AppUser usuario = destinatario.get();
        RelatorioSemanal relatorio = relatorioSemanalService.daSemana(LocalDate.now(ZoneId.of(fuso)));
        EmailMensagem mensagem = new EmailMensagem(
                usuario.getEmail().trim(),
                assunto(relatorio),
                montarHtml(relatorio, usuario),
                textoSimples(relatorio));

        try {
            envioEmail.enviar(mensagem);

            log.info("Relatório semanal ({}) enviado para {} ({} show(s)).",
                    relatorio.periodo(), usuario.getEmail(), relatorio.quantidade());
        } catch (EnvioEmailException ex) {
            log.error("Falha ao enviar o relatório semanal para {}.", usuario.getEmail(), ex);
        }
    }

    private String assunto(RelatorioSemanal relatorio) {
        return "Shows de %s a %s".formatted(
                DIA_MES.format(relatorio.inicio()), DIA_MES.format(relatorio.fim()));
    }

    private String montarHtml(RelatorioSemanal relatorio, AppUser usuario) {
        Context contexto = new Context(Locale.forLanguageTag("pt-BR"));
        contexto.setVariable("relatorio", relatorio);
        contexto.setVariable("nomeUsuario", usuario.getNomeUsuario());
        return templateEngine.process(TEMPLATE, contexto);
    }

    private static String textoSimples(RelatorioSemanal relatorio) {
        StringBuilder texto = new StringBuilder()
                .append("Shows de ").append(relatorio.periodo()).append("\n\n");

        if (relatorio.shows().isEmpty()) {
            texto.append("Nenhum show agendado para esta semana.\n");
            return texto.toString();
        }

        for (ShowRelatorio show : relatorio.shows()) {
            texto.append(show.data())
                    .append(" ").append(show.horario())
                    .append(" - ").append(show.artista())
                    .append(" em ").append(show.local())
                    .append(" | ").append(show.cache())
                    .append(" | ").append(show.situacao())
                    .append('\n');
        }

        texto.append("\nTotal: ").append(relatorio.quantidade()).append(" show(s)")
                .append(" | Cachê total: ").append(relatorio.cacheTotal())
                .append(" | Pagos: ").append(relatorio.quantidadePagas())
                .append(" | Pendentes: ").append(relatorio.quantidadePendentes())
                .append('\n');
        return texto.toString();
    }
}
