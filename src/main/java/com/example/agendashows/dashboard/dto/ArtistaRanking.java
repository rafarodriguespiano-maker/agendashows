package com.example.agendashows.dashboard.dto;

import java.math.BigDecimal;

/**
 * Posição de um artista no ranking do período: quantidade de shows e cachê acumulado.
 */
public record ArtistaRanking(String artista, long quantidadeShows, BigDecimal cacheTotal) {
}
