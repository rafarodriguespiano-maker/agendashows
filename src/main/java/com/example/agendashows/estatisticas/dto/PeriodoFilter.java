package com.example.agendashows.estatisticas.dto;

import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Filtro de mês e ano da tela de estatísticas.
 *
 * <p>Quando nenhum dos campos é informado, o período atual é utilizado como padrão.</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class PeriodoFilter {

    private Integer mes;
    private Integer ano;

    /**
     * Preenche mês e ano com o período corrente quando ambos estão vazios.
     */
    public void preencherPeriodoAtualSeVazio() {
        if (mes == null && ano == null) {
            LocalDate hoje = LocalDate.now();
            mes = hoje.getMonthValue();
            ano = hoje.getYear();
        }
    }

    public Integer mesValido() {
        return mes != null && mes >= 1 && mes <= 12 ? mes : null;
    }

    public Integer anoValido() {
        return ano != null && ano >= 1 && ano <= 9999 ? ano : null;
    }
}
