package com.example.agendashows.show.exception;

public class ShowNotFoundException extends RuntimeException {

    public ShowNotFoundException(Long id) {
        super("Show não encontrado: " + id);
    }
}
