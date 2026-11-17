package com.example.agendashows.relatorio.email;

/**
 * Falha ao entregar a mensagem ao provedor de e-mail.
 */
public class EnvioEmailException extends RuntimeException {

    public EnvioEmailException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }

    public EnvioEmailException(String mensagem) {
        super(mensagem);
    }
}
