package com.example.agendashows.relatorio.service;

import com.example.agendashows.relatorio.dto.RelatorioSemanal;
import com.example.agendashows.relatorio.dto.ShowRelatorio;
import com.example.agendashows.user.entity.AppUser;
import com.example.agendashows.user.repository.AppUserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Envia o relatório semanal para o usuário principal (primeiro usuário ativo com e-mail cadastrado).
 * Só é registrado quando o SMTP está configurado ({@code spring.mail.host}).
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "spring.mail", name = "host")
public class RelatorioEmailService {

    private static final String TEMPLATE = "email/relatorio-semanal";
    private static final DateTimeFormatter DIA_MES = DateTimeFormatter.ofPattern("dd/MM", Locale.forLanguageTag("pt-BR"));

    private final RelatorioSemanalService relatorioSemanalService;
    private final AppUserRepository appUserRepository;
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final String remetente;
    private final String fuso;

    public RelatorioEmailService(RelatorioSemanalService relatorioSemanalService,
                                 AppUserRepository appUserRepository,
                                 JavaMailSender mailSender,
                                 TemplateEngine templateEngine,
                                 @Value("${app.relatorio-email.remetente:}") String remetente,
                                 @Value("${app.relatorio-email.fuso:America/Sao_Paulo}") String fuso) {
        this.relatorioSemanalService = relatorioSemanalService;
        this.appUserRepository = appUserRepository;
        this.mailSender = mailSender;
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

        try {
            MimeMessage mensagem = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensagem, true, "UTF-8");
            helper.setFrom(remetente);
            helper.setTo(usuario.getEmail().trim());
            helper.setSubject(assunto(relatorio));
            helper.setText(textoSimples(relatorio), montarHtml(relatorio, usuario));
            mailSender.send(mensagem);

            log.info("Relatório semanal ({}) enviado para {} ({} show(s)).",
                    relatorio.periodo(), usuario.getEmail(), relatorio.quantidade());
        } catch (MessagingException | MailException ex) {
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
