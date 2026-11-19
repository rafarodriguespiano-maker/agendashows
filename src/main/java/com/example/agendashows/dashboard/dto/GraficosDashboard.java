package com.example.agendashows.dashboard.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Dados dos gráficos: totais mensais do ano de referência, cachês médios mensais por forma de pagamento,
 * participação por artista e totais/médias por ano (todos os anos cadastrados).
 *
 * <p>As listas mensais sempre possuem 12 posições, preenchidas com zero quando não há shows no mês.</p>
 */
public record GraficosDashboard(
        int ano,
        List<Long> showsPorMes,
        List<BigDecimal> cacheMedioAVistaPorMes,
        List<BigDecimal> cacheMedioMensalPorMes,
        List<String> artistas,
        List<Long> showsPorArtista,
        List<Integer> anos,
        List<Long> showsPorAno,
        List<BigDecimal> cacheMedioAVistaPorAno,
        List<BigDecimal> cacheMedioMensalPorAno) {
}
