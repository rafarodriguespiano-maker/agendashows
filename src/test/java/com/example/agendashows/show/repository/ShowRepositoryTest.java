package com.example.agendashows.show.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.agendashows.show.dto.ShowFilter;
import com.example.agendashows.show.entity.enums.StatusPagamento;
import com.example.agendashows.show.entity.Show;
import com.example.agendashows.show.specification.ShowSpecifications;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class ShowRepositoryTest {

    @Autowired
    private ShowRepository showRepository;

    @BeforeEach
    void prepararDados() {
        showRepository.deleteAll();
        showRepository.saveAll(List.of(
                criar(LocalDate.of(2026, 3, 10), "Banda A", "Bar do Zé", "Ótimas notas", true),
                criar(LocalDate.of(2026, 3, 20), "Banda B", "Teatro Municipal", null, false),
                criar(LocalDate.of(2026, 4, 5), "Banda C", "Bar do Zé", "Show acústico", false),
                criar(LocalDate.of(2025, 3, 15), "Banda D", "Praça Central", "Tarde de festa", true)));
    }

    @Test
    void filtraPorMesEAno() {
        assertThat(buscar(filtro(3, 2026, null, null, null))).hasSize(2);
    }

    @Test
    void filtraPorAnoInteiro() {
        assertThat(buscar(filtro(null, 2026, null, null, null))).hasSize(3);
    }

    @Test
    void filtraApenasPorMesEmQualquerAno() {
        assertThat(buscar(filtro(3, null, null, null, null))).hasSize(3);
    }

    @Test
    void filtraPorSituacaoDePagamento() {
        assertThat(buscar(filtro(null, null, StatusPagamento.PAGO, null, null))).hasSize(2);
        assertThat(buscar(filtro(null, null, StatusPagamento.NAO_PAGO, null, null))).hasSize(2);
    }

    @Test
    void filtraPorArtistaParcialSemDiferenciarMaiusculas() {
        ShowFilter filtro = new ShowFilter();
        filtro.setArtista("banda b");

        assertThat(buscar(filtro)).hasSize(1);
        assertThat(buscar(filtro).getFirst().getArtista()).isEqualTo("Banda B");
    }

    @Test
    void filtraPorObservacoesSemDiferenciarMaiusculas() {
        assertThat(buscar(filtro(null, null, null, "ACÚSTICO", null))).hasSize(1);
    }

    @Test
    void filtraPorLocalParcial() {
        assertThat(buscar(filtro(null, null, null, null, "bar do"))).hasSize(2);
    }

    @Test
    void combinaFiltros() {
        assertThat(buscar(filtro(3, 2026, StatusPagamento.PAGO, null, "Bar"))).hasSize(1);
    }

    @Test
    void pagina() {
        Page<Show> pagina = showRepository.findAll(ShowSpecifications.comFiltros(new ShowFilter()),
                PageRequest.of(0, 2, Sort.by("data")));

        assertThat(pagina.getTotalElements()).isEqualTo(4);
        assertThat(pagina.getTotalPages()).isEqualTo(2);
        assertThat(pagina.getContent()).hasSize(2);
    }

    private List<Show> buscar(ShowFilter filtro) {
        return showRepository.findAll(ShowSpecifications.comFiltros(filtro));
    }

    private ShowFilter filtro(Integer mes, Integer ano, StatusPagamento situacao, String observacoes, String local) {
        ShowFilter filtro = new ShowFilter();
        filtro.setMes(mes);
        filtro.setAno(ano);
        filtro.setSituacaoPagamento(situacao);
        filtro.setObservacoes(observacoes);
        filtro.setLocal(local);
        return filtro;
    }

    private Show criar(LocalDate data, String artista, String local, String observacoes, boolean pago) {
        Show show = new Show();
        show.setData(data);
        show.setArtista(artista);
        show.setLocal(local);
        show.setObservacoes(observacoes);
        show.setCache(new BigDecimal("1000.00"));
        show.setPago(pago);
        return show;
    }
}
