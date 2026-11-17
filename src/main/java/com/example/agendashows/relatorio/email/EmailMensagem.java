package com.example.agendashows.relatorio.email;

/**
 * Mensagem de e-mail já renderizada, pronta para qualquer transporte.
 */
public record EmailMensagem(
        String destinatario,
        String assunto,
        String html,
        String texto) {
}
