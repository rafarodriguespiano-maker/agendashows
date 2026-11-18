package com.example.agendashows.dashboard.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Dados dos gráficos: totais mensais do ano de referência, cachê médio mensal, participação por artista
 * e totais por ano (todos os anos cadastrados).
 *
 * <p>As listas mensais sempre possuem 12 posições, preenchidas com zero quando não há shows no mês.</p>
 */
public record GraficosDashboard(
        int ano,
        List<Long> showsPorMes,
        List<BigDecimal> cacheMedioPorMes,
        List<String> artistas,
        List<Long> showsPorArtista,
        List<Integer> anos,
        List<Long> showsPorAno,
        List<BigDecimal> cacheMedioPorAno) {
}
