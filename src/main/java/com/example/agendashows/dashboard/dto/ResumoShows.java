package com.example.agendashows.dashboard.dto;

import java.math.BigDecimal;

/**
 * Totais agregados dos shows de um período.
 *
 * <p>{@code aVista} e {@code mensal} separam as dashboard por forma de pagamento;
 * {@code quantidadeGeral} e {@code cacheGeral} somam todos os shows do período.</p>
 */
public record ResumoShows(
        ResumoPagamento aVista,
        ResumoPagamento mensal,
        long quantidadeGeral,
        BigDecimal cacheGeral) {
}
