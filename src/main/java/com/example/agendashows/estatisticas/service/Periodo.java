package com.example.agendashows.estatisticas.service;

import com.example.agendashows.estatisticas.dto.PeriodoFilter;
import java.time.LocalDate;

/**
 * Intervalo de datas efetivamente aplicado nas consultas de estatísticas.
 *
 * <p>Somente um dos critérios é usado por consulta: ou o intervalo exato ({@code inicio}/{@code fim}),
 * ou o mês isolado em qualquer ano ({@code mes}).</p>
 */
record Periodo(LocalDate inicio, LocalDate fim, Integer mes) {

    static Periodo de(PeriodoFilter filtro) {
        Integer mes = filtro.mesValido();
        Integer ano = filtro.anoValido();

        if (mes == null && ano == null) {
            LocalDate hoje = LocalDate.now();
            mes = hoje.getMonthValue();
            ano = hoje.getYear();
        }

        if (ano == null) {
            return new Periodo(null, null, mes);
        }

        LocalDate inicio = LocalDate.of(ano, mes == null ? 1 : mes, 1);
        LocalDate fim = mes == null ? inicio.plusYears(1) : inicio.plusMonths(1);
        return new Periodo(inicio, fim, null);
    }
}
