package com.example.agendashows.show.dto;

import com.example.agendashows.show.entity.enums.StatusPagamento;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ShowFilter {

    private Integer mes;
    private Integer ano;
    private StatusPagamento situacaoPagamento;
    private String artista;
    private String observacoes;
    private String local;
}
