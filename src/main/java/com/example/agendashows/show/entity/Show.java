package com.example.agendashows.show.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "shows")
@Getter
@Setter
@NoArgsConstructor
public class Show {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "data", nullable = false)
    private LocalDate data;

    @Column(name = "artista", nullable = false, length = 150)
    private String artista;

    @Column(name = "local", nullable = false, length = 200)
    private String local;

    @Column(name = "horario")
    private LocalTime horario;

    @Column(name = "observacoes", length = 2000)
    private String observacoes;

    @Column(name = "cache", precision = 10, scale = 2)
    private BigDecimal cache;

    @Column(name = "pago", nullable = false)
    private boolean pago;

    @Column(name = "pagamento_mensal", nullable = false)
    private boolean pagamentoMensal;

    // Show ocorrido em data anterior a hoje; os de hoje permanecem em destaque.
    public boolean isPassado() {
        return data != null && data.isBefore(LocalDate.now());
    }
}
