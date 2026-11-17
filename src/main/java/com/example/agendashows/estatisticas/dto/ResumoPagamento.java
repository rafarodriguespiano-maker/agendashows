package com.example.agendashows.estatisticas.dto;

import java.math.BigDecimal;

/**
 * Totais de um grupo de shows do período, separados por forma de pagamento.
 */
public record ResumoPagamento(
        long quantidade,
        long quantidadePagas,
        long quantidadePendentes,
        BigDecimal cacheTotal,
        BigDecimal cachePago,
        BigDecimal cachePendente,
        BigDecimal cacheMedio,
        BigDecimal maiorCache,
        BigDecimal menorCache) {
}
