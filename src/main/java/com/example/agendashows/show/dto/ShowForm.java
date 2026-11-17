package com.example.agendashows.show.dto;

import com.example.agendashows.show.entity.Show;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ShowForm {

    private Long id;

    @NotNull(message = "Informe a data do show.")
    private LocalDate data;

    @NotBlank(message = "Informe o nome do artista.")
    @Size(max = 150, message = "O artista deve ter no máximo 150 caracteres.")
    private String artista;

    @NotBlank(message = "Informe o local do show.")
    @Size(max = 200, message = "O local deve ter no máximo 200 caracteres.")
    private String local;

    private LocalTime horario;

    @Size(max = 2000, message = "As observações devem ter no máximo 2000 caracteres.")
    private String observacoes;

    @DecimalMin(value = "0.00", message = "O cachê não pode ser negativo.")
    @Digits(integer = 8, fraction = 2, message = "O cachê deve ter até 8 dígitos inteiros e 2 decimais.")
    private BigDecimal cache;

    private boolean pago;

    private boolean pagamentoMensal;

    public static ShowForm de(Show show) {
        ShowForm formulario = new ShowForm();
        formulario.setId(show.getId());
        formulario.setData(show.getData());
        formulario.setArtista(show.getArtista());
        formulario.setLocal(show.getLocal());
        formulario.setHorario(show.getHorario());
        formulario.setObservacoes(show.getObservacoes());
        formulario.setCache(show.getCache());
        formulario.setPago(show.isPago());
        formulario.setPagamentoMensal(show.isPagamentoMensal());
        return formulario;
    }

    public void aplicarEm(Show show) {
        show.setData(data);
        show.setArtista(artista);
        show.setLocal(local);
        show.setHorario(horario);
        show.setObservacoes(observacoes);
        show.setCache(cache);
        show.setPago(pago);
        show.setPagamentoMensal(pagamentoMensal);
    }
}
