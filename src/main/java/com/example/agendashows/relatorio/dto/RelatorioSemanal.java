package com.example.agendashows.relatorio.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Dados do relatório semanal: shows da segunda-feira até o domingo seguinte.
 */
public record RelatorioSemanal(
        LocalDate inicio,
        LocalDate fim,
        String periodo,
        List<ShowRelatorio> shows,
        String cacheTotal,
        long quantidadePagas,
        long quantidadePendentes) {

    public int quantidade() {
        return shows.size();
    }
}
