package com.example.agendashows.show.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Formulário de cadastro de vários shows de uma só vez, a partir do mês/ano e dos dias da semana
 * selecionados. Horário, cachê, artista, local, observações e pagamento mensal são copiados para
 * cada show gerado.
 */
@Getter
@Setter
@NoArgsConstructor
public class ShowLoteForm {

    @NotNull(message = "Informe o mês.")
    @Min(value = 1, message = "O mês deve estar entre 1 e 12.")
    @Max(value = 12, message = "O mês deve estar entre 1 e 12.")
    private Integer mes;

    @NotNull(message = "Informe o ano.")
    @Min(value = 1, message = "O ano deve estar entre 1 e 9999.")
    @Max(value = 9999, message = "O ano deve estar entre 1 e 9999.")
    private Integer ano;

    private boolean domingo;

    private boolean segunda;

    private boolean terca;

    private boolean quarta;

    private boolean quinta;

    private boolean sexta;

    private boolean sabado;

    private LocalTime horario;

    @DecimalMin(value = "0.00", message = "O cachê não pode ser negativo.")
    @Digits(integer = 8, fraction = 2, message = "O cachê deve ter até 8 dígitos inteiros e 2 decimais.")
    private BigDecimal cache;

    @NotBlank(message = "Informe o nome do artista.")
    @Size(max = 150, message = "O artista deve ter no máximo 150 caracteres.")
    private String artista;

    @NotBlank(message = "Informe o local do show.")
    @Size(max = 200, message = "O local deve ter no máximo 200 caracteres.")
    private String local;

    @Size(max = 2000, message = "As observações devem ter no máximo 2000 caracteres.")
    private String observacoes;

    private boolean pagamentoMensal;

    public List<DayOfWeek> diasSelecionados() {
        List<DayOfWeek> dias = new ArrayList<>();
        if (domingo) {
            dias.add(DayOfWeek.SUNDAY);
        }
        if (segunda) {
            dias.add(DayOfWeek.MONDAY);
        }
        if (terca) {
            dias.add(DayOfWeek.TUESDAY);
        }
        if (quarta) {
            dias.add(DayOfWeek.WEDNESDAY);
        }
        if (quinta) {
            dias.add(DayOfWeek.THURSDAY);
        }
        if (sexta) {
            dias.add(DayOfWeek.FRIDAY);
        }
        if (sabado) {
            dias.add(DayOfWeek.SATURDAY);
        }
        return dias;
    }

    @AssertTrue(message = "Selecione ao menos um dia da semana.")
    public boolean isAoMenosUmDiaSelecionado() {
        return !diasSelecionados().isEmpty();
    }
}
