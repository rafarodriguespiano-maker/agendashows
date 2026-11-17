package com.example.agendashows.relatorio.email;

/**
 * Transporte de e-mail: SMTP (VPS/desenvolvimento) ou API HTTPS (Railway Free/Trial/Hobby).
 */
public interface EnvioEmail {

    void enviar(EmailMensagem mensagem);
}
