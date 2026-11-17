package com.example.agendashows.relatorio.service;

import com.example.agendashows.relatorio.dto.RelatorioSemanal;
import com.example.agendashows.relatorio.dto.ShowRelatorio;
import com.example.agendashows.show.entity.Show;
import com.example.agendashows.show.repository.ShowRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Monta o relatório com os shows da semana corrente: da segunda-feira de referência até o domingo seguinte.
 */
@Service
@RequiredArgsConstructor
public class RelatorioSemanalService {

    private static final Locale BRASIL = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter FORMATO_PERIODO =
            DateTimeFormatter.ofPattern("dd/MM/yyyy", BRASIL);
    private static final DateTimeFormatter FORMATO_DATA =
            DateTimeFormatter.ofPattern("EEE, dd/MM", BRASIL);
    private static final DateTimeFormatter FORMATO_HORARIO =
            DateTimeFormatter.ofPattern("HH:mm", BRASIL);
    private static final String SEM_VALOR = "—";

    private final ShowRepository showRepository;

    @Transactional(readOnly = true)
    public RelatorioSemanal daSemana(LocalDate referencia) {
        LocalDate inicio = inicioDaSemana(referencia);
        LocalDate fim = inicio.plusDays(6);

        List<Show> shows = showRepository.findByDataBetweenOrderByDataAscHorarioAsc(inicio, fim);

        long pagas = shows.stream().filter(Show::isPago).count();
        BigDecimal cacheTotal = shows.stream()
                .map(Show::getCache)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        return new RelatorioSemanal(
                inicio,
                fim,
                FORMATO_PERIODO.format(inicio) + " a " + FORMATO_PERIODO.format(fim),
                shows.stream().map(this::paraLinha).toList(),
                formatarMoeda(cacheTotal),
                pagas,
                shows.size() - pagas);
    }

    static LocalDate inicioDaSemana(LocalDate referencia) {
        return referencia.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private ShowRelatorio paraLinha(Show show) {
        return new ShowRelatorio(
                FORMATO_DATA.format(show.getData()),
                show.getHorario() == null ? SEM_VALOR : FORMATO_HORARIO.format(show.getHorario()),
                show.getArtista(),
                show.getLocal(),
                show.getCache() == null ? SEM_VALOR : formatarMoeda(show.getCache()),
                show.isPago() ? "Pago" : "Pendente",
                show.getObservacoes() == null || show.getObservacoes().isBlank()
                        ? SEM_VALOR
                        : show.getObservacoes());
    }

    private static String formatarMoeda(BigDecimal valor) {
        NumberFormat formato = NumberFormat.getCurrencyInstance(BRASIL);
        return formato.format(valor);
    }
}
