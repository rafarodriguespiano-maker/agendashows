package com.example.agendashows.relatorio.dto;

/**
 * Linha do relatório semanal já formatada para exibição no e-mail.
 */
public record ShowRelatorio(
        String data,
        String horario,
        String artista,
        String local,
        String cache,
        String situacao,
        String observacoes) {
}
