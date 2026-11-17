package com.example.agendashows.estatisticas.dto;

import java.math.BigDecimal;

/**
 * Totais agregados dos shows de um período.
 */
public record ResumoShows(
        long quantidadeShows,
        BigDecimal cacheTotal,
        BigDecimal cachePago,
        BigDecimal cachePendente,
        long quantidadePagas,
        long quantidadePendentes,
        BigDecimal cacheMedio,
        BigDecimal maiorCache) {
}
