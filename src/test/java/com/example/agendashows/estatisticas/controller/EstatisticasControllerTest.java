package com.example.agendashows.estatisticas.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.example.agendashows.estatisticas.dto.ArtistaRanking;
import com.example.agendashows.estatisticas.dto.ResumoShows;
import com.example.agendashows.show.entity.Show;
import com.example.agendashows.show.repository.ShowRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EstatisticasControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ShowRepository showRepository;

    @BeforeEach
    void limparBanco() {
        showRepository.deleteAll();
    }

    @Test
    void redirecionaParaLoginSemAutenticacao() throws Exception {
        mvc.perform(get("/estatisticas"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/entrar"));
    }

    @Test
    @WithMockUser
    void exibeTotaisDoPeriodoFiltrado() throws Exception {
        salvar(LocalDate.of(2026, 3, 10), "Banda A", "1000.00", true);
        salvar(LocalDate.of(2026, 3, 20), "Banda A", "500.00", false);
        salvar(LocalDate.of(2026, 4, 5), "Banda B", "300.00", true);

        mvc.perform(get("/estatisticas").param("mes", "3").param("ano", "2026"))
                .andExpect(status().isOk())
                .andExpect(view().name("estatisticas/index"))
                .andExpect(model().attribute("resumo", is(new ResumoShows(
                        2,
                        new BigDecimal("1500.00"),
                        new BigDecimal("1000.00"),
                        new BigDecimal("500.00"),
                        1,
                        1,
                        new BigDecimal("750.00"),
                        new BigDecimal("1000.00")))))
                .andExpect(content().string(containsString("Estatísticas")))
                .andExpect(content().string(containsString("Banda A")))
                .andExpect(content().string(not(containsString("Banda B"))));
    }

    @Test
    @WithMockUser
    void exibeRankingDeArtistasDoPeriodo() throws Exception {
        salvar(LocalDate.of(2026, 3, 10), "Banda A", "1000.00", true);
        salvar(LocalDate.of(2026, 3, 20), "Banda A", "500.00", false);
        salvar(LocalDate.of(2026, 3, 25), "Banda B", "800.00", true);

        mvc.perform(get("/estatisticas").param("mes", "3").param("ano", "2026"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("ranking", is(List.of(
                        new ArtistaRanking("Banda A", 2, new BigDecimal("1500.00")),
                        new ArtistaRanking("Banda B", 1, new BigDecimal("800.00"))))))
                .andExpect(content().string(containsString("Ranking de artistas")))
                .andExpect(content().string(containsString("Banda A")));
    }

    @Test
    @WithMockUser
    void preencheMesEAnoAtuaisQuandoNaoInformados() throws Exception {
        LocalDate hoje = LocalDate.now();
        salvar(hoje, "Banda Atual", "500.00", true);
        salvar(hoje.minusYears(2), "Banda Antiga", "500.00", true);

        mvc.perform(get("/estatisticas"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("filtro", hasProperty("mes", is(hoje.getMonthValue()))))
                .andExpect(model().attribute("filtro", hasProperty("ano", is(hoje.getYear()))))
                .andExpect(model().attribute("resumo", is(new ResumoShows(
                        1,
                        new BigDecimal("500.00"),
                        new BigDecimal("500.00"),
                        new BigDecimal("0.00"),
                        1,
                        0,
                        new BigDecimal("500.00"),
                        new BigDecimal("500.00")))))
                .andExpect(content().string(containsString("Banda Atual")))
                .andExpect(content().string(not(containsString("Banda Antiga"))));
    }

    @Test
    @WithMockUser
    void filtraApenasPorMesEmQualquerAno() throws Exception {
        salvar(LocalDate.of(2025, 3, 10), "Banda 2025", "100.00", true);
        salvar(LocalDate.of(2026, 3, 10), "Banda 2026", "200.00", true);
        salvar(LocalDate.of(2026, 4, 10), "Banda Abril", "300.00", true);

        mvc.perform(get("/estatisticas").param("mes", "3"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("resumo", is(new ResumoShows(
                        2,
                        new BigDecimal("300.00"),
                        new BigDecimal("300.00"),
                        new BigDecimal("0.00"),
                        2,
                        0,
                        new BigDecimal("150.00"),
                        new BigDecimal("200.00")))));
    }

    @Test
    @WithMockUser
    void filtraAnoInteiroQuandoMesNaoInformado() throws Exception {
        salvar(LocalDate.of(2025, 12, 10), "Banda 2025", "100.00", true);
        salvar(LocalDate.of(2026, 3, 10), "Banda Março", "200.00", true);
        salvar(LocalDate.of(2026, 4, 10), "Banda Abril", "300.00", true);

        mvc.perform(get("/estatisticas").param("ano", "2026"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("resumo", is(new ResumoShows(
                        2,
                        new BigDecimal("500.00"),
                        new BigDecimal("500.00"),
                        new BigDecimal("0.00"),
                        2,
                        0,
                        new BigDecimal("250.00"),
                        new BigDecimal("300.00")))));
    }

    @Test
    @WithMockUser
    void exibeEstadoVazioQuandoNaoHaShowsNoPeriodo() throws Exception {
        salvar(LocalDate.of(2026, 4, 5), "Banda B", "300.00", true);

        mvc.perform(get("/estatisticas").param("mes", "3").param("ano", "2026"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("resumo", is(new ResumoShows(
                        0,
                        new BigDecimal("0.00"),
                        new BigDecimal("0.00"),
                        new BigDecimal("0.00"),
                        0,
                        0,
                        new BigDecimal("0.00"),
                        new BigDecimal("0.00")))))
                .andExpect(model().attribute("ranking", is(List.of())))
                .andExpect(content().string(containsString("Nenhum show encontrado no período.")));
    }

    @Test
    @WithMockUser
    void consideraShowsSemCacheComoZeroNosTotais() throws Exception {
        salvar(LocalDate.of(2026, 3, 10), "Banda Sem Cachê", null, false);

        mvc.perform(get("/estatisticas").param("mes", "3").param("ano", "2026"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("resumo", is(new ResumoShows(
                        1,
                        new BigDecimal("0.00"),
                        new BigDecimal("0.00"),
                        new BigDecimal("0.00"),
                        0,
                        1,
                        new BigDecimal("0.00"),
                        new BigDecimal("0.00")))))
                .andExpect(model().attribute("ranking", is(List.of(
                        new ArtistaRanking("Banda Sem Cachê", 1, new BigDecimal("0.00"))))));
    }

    private Show salvar(LocalDate data, String artista, String cache, boolean pago) {
        Show show = new Show();
        show.setData(data);
        show.setArtista(artista);
        show.setLocal("Local");
        show.setCache(cache == null ? null : new BigDecimal(cache));
        show.setPago(pago);
        return showRepository.save(show);
    }
}
