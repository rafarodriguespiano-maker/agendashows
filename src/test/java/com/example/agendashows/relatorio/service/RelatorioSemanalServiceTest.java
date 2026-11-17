package com.example.agendashows.relatorio.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.agendashows.relatorio.dto.RelatorioSemanal;
import com.example.agendashows.relatorio.dto.ShowRelatorio;
import com.example.agendashows.show.entity.Show;
import com.example.agendashows.show.repository.ShowRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class RelatorioSemanalServiceTest {

    @Autowired
    private ShowRepository showRepository;

    private RelatorioSemanalService relatorioSemanalService;

    @BeforeEach
    void prepararDados() {
        relatorioSemanalService = new RelatorioSemanalService(showRepository);
        showRepository.deleteAll();
        showRepository.saveAll(List.of(
                criar(LocalDate.of(2026, 3, 8), "Domingo anterior", true, "800.00"),
                criar(LocalDate.of(2026, 3, 9), "Segunda", true, "1000.00"),
                criar(LocalDate.of(2026, 3, 15), "Domingo", false, "500.00"),
                criar(LocalDate.of(2026, 3, 16), "Segunda seguinte", false, "700.00")));
    }

    @Test
    void consideraDaSegundaAoDomingo() {
        RelatorioSemanal relatorio = relatorioSemanalService.daSemana(LocalDate.of(2026, 3, 11));

        assertThat(relatorio.inicio()).isEqualTo(LocalDate.of(2026, 3, 9));
        assertThat(relatorio.fim()).isEqualTo(LocalDate.of(2026, 3, 15));
        assertThat(relatorio.periodo()).isEqualTo("09/03/2026 a 15/03/2026");
        assertThat(relatorio.shows()).extracting(ShowRelatorio::artista)
                .containsExactly("Segunda", "Domingo");
        assertThat(relatorio.quantidade()).isEqualTo(2);
        assertThat(relatorio.cacheTotal()).contains("1.500,00");
        assertThat(relatorio.quantidadePagas()).isEqualTo(1);
        assertThat(relatorio.quantidadePendentes()).isEqualTo(1);
    }

    @Test
    void referenciaNaPropriaSegundaUsaAMesmaSemana() {
        RelatorioSemanal relatorio = relatorioSemanalService.daSemana(LocalDate.of(2026, 3, 9));

        assertThat(relatorio.inicio()).isEqualTo(LocalDate.of(2026, 3, 9));
        assertThat(relatorio.fim()).isEqualTo(LocalDate.of(2026, 3, 15));
    }

    @Test
    void referenciaNoDomingoUsaASegundaDaSemana() {
        RelatorioSemanal relatorio = relatorioSemanalService.daSemana(LocalDate.of(2026, 3, 15));

        assertThat(relatorio.inicio()).isEqualTo(LocalDate.of(2026, 3, 9));
    }

    @Test
    void semanaSemShowsRetornaRelatorioVazio() {
        RelatorioSemanal relatorio = relatorioSemanalService.daSemana(LocalDate.of(2026, 6, 1));

        assertThat(relatorio.shows()).isEmpty();
        assertThat(relatorio.quantidade()).isZero();
        assertThat(relatorio.cacheTotal()).contains("0,00");
    }

    private Show criar(LocalDate data, String artista, boolean pago, String cache) {
        Show show = new Show();
        show.setData(data);
        show.setArtista(artista);
        show.setLocal("Local " + artista);
        show.setHorario(LocalTime.of(20, 0));
        show.setCache(new BigDecimal(cache));
        show.setPago(pago);
        return show;
    }
}
