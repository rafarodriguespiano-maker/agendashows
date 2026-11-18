package com.example.agendashows.calendario.service;

import com.example.agendashows.show.entity.Show;
import com.example.agendashows.show.repository.ShowRepository;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Gera um feed iCalendar (RFC 5545) com os shows a partir de hoje, para assinatura no Apple Calendar.
 */
@Service
public class CalendarioIcsService {

    private static final String FIM_DE_LINHA = "\r\n";
    private static final int LIMITE_LINHA = 75;
    private static final DateTimeFormatter FORMATO_UTC =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.BASIC_ISO_DATE;

    private final ShowRepository showRepository;
    private final ZoneId fuso;
    private final Duration duracaoPadrao;

    public CalendarioIcsService(ShowRepository showRepository,
                                @Value("${app.calendario.fuso:America/Sao_Paulo}") String fuso,
                                @Value("${app.calendario.duracao-padrao-minutos:180}") long duracaoPadraoMinutos) {
        this.showRepository = showRepository;
        this.fuso = ZoneId.of(fuso);
        this.duracaoPadrao = Duration.ofMinutes(duracaoPadraoMinutos);
    }

    @Transactional(readOnly = true)
    public String gerarFeed() {
        List<Show> shows = showRepository.buscarProximos(LocalDate.now(fuso), null, null, null);

        StringBuilder ics = new StringBuilder(1024);
        adicionarLinha(ics, "BEGIN:VCALENDAR");
        adicionarLinha(ics, "VERSION:2.0");
        adicionarLinha(ics, "PRODID:-//Agenda de Shows//Calendario//PT-BR");
        adicionarLinha(ics, "CALSCALE:GREGORIAN");
        adicionarLinha(ics, "METHOD:PUBLISH");
        adicionarLinha(ics, "X-WR-CALNAME:Agenda de Shows");
        adicionarLinha(ics, "X-WR-CALDESC:Shows cadastrados na Agenda de Shows");
        adicionarLinha(ics, "X-WR-TIMEZONE:" + fuso.getId());
        adicionarLinha(ics, "REFRESH-INTERVAL;VALUE=DURATION:PT1H");
        shows.forEach(show -> adicionarEvento(ics, show));
        adicionarLinha(ics, "END:VCALENDAR");
        return ics.toString();
    }

    private void adicionarEvento(StringBuilder ics, Show show) {
        adicionarLinha(ics, "BEGIN:VEVENT");
        adicionarLinha(ics, "UID:show-" + show.getId() + "@agendashows");
        adicionarLinha(ics, "DTSTAMP:" + FORMATO_UTC.format(Instant.now()));

        if (show.getHorario() != null) {
            // Datas em UTC: dispensam o bloco VTIMEZONE e funcionam em qualquer cliente.
            ZonedDateTime inicio = show.getData().atTime(show.getHorario()).atZone(fuso);
            adicionarLinha(ics, "DTSTART:" + FORMATO_UTC.format(inicio.toInstant()));
            adicionarLinha(ics, "DTEND:" + FORMATO_UTC.format(inicio.plus(duracaoPadrao).toInstant()));
        } else {
            // Sem horário: evento de dia inteiro (o DTEND é exclusivo, por isso o dia seguinte).
            adicionarLinha(ics, "DTSTART;VALUE=DATE:" + FORMATO_DATA.format(show.getData()));
            adicionarLinha(ics, "DTEND;VALUE=DATE:" + FORMATO_DATA.format(show.getData().plusDays(1)));
        }

        adicionarLinha(ics, "SUMMARY:" + escapar(show.getArtista()));
        if (StringUtils.hasText(show.getLocal())) {
            adicionarLinha(ics, "LOCATION:" + escapar(show.getLocal()));
        }
        if (StringUtils.hasText(show.getObservacoes())) {
            adicionarLinha(ics, "DESCRIPTION:" + escapar(show.getObservacoes()));
        }
        adicionarLinha(ics, "STATUS:CONFIRMED");
        adicionarLinha(ics, "END:VEVENT");
    }

    /**
     * Escreve uma linha já com a dobra (folding) exigida pelo RFC 5545: no máximo 75 octetos por linha física.
     */
    private void adicionarLinha(StringBuilder ics, String conteudo) {
        String texto = conteudo == null ? "" : conteudo;
        int limite = LIMITE_LINHA;
        int bytesNaLinha = 0;
        StringBuilder linha = new StringBuilder();

        for (int i = 0; i < texto.length(); ) {
            int codigo = texto.codePointAt(i);
            String caractere = new String(Character.toChars(codigo));
            int bytes = caractere.getBytes(StandardCharsets.UTF_8).length;

            if (bytesNaLinha + bytes > limite) {
                ics.append(linha).append(FIM_DE_LINHA);
                linha.setLength(0);
                bytesNaLinha = 0;
                // A continuação começa com um espaço, que também conta no limite de 75 octetos.
                linha.append(' ');
                bytesNaLinha = 1;
                limite = LIMITE_LINHA;
            }

            linha.append(caractere);
            bytesNaLinha += bytes;
            i += Character.charCount(codigo);
        }

        ics.append(linha).append(FIM_DE_LINHA);
    }

    private String escapar(String valor) {
        if (valor == null) {
            return "";
        }
        return valor
                .replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\r\n", "\\n")
                .replace("\n", "\\n")
                .replace("\r", "\\n");
    }
}
