package com.example.agendashows.estatisticas.dto;

import java.math.BigDecimal;

/**
 * Totais agregados dos shows de um período.
 *
 * <p>{@code aVista} e {@code mensal} separam as estatísticas por forma de pagamento;
 * {@code quantidadeGeral} e {@code cacheGeral} somam todos os shows do período.</p>
 */
public record ResumoShows(
        ResumoPagamento aVista,
        ResumoPagamento mensal,
        long quantidadeGeral,
        BigDecimal cacheGeral) {
}
