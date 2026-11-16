package com.example.agendashows.show.entity.enums;

import lombok.Getter;

@Getter
public enum StatusPagamento {

    PAGO("Pago"),
    NAO_PAGO("Não pago");

    private final String descricao;

    StatusPagamento(String descricao) {
        this.descricao = descricao;
    }
}
