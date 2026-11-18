package com.example.agendashows.dashboard.controller;

import static org.hamcrest.Matchers.contains;
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

import com.example.agendashows.dashboard.dto.ArtistaRanking;
import com.example.agendashows.dashboard.dto.GraficosDashboard;
import com.example.agendashows.dashboard.dto.ResumoPagamento;
import com.example.agendashows.dashboard.dto.ResumoShows;
import com.example.agendashows.show.entity.Show;
import com.example.agendashows.show.repository.ShowRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
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
class DashboardControllerTest {

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
        mvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/entrar"));
    }

    @Test
    @WithMockUser
    void exibeTotaisDoPeriodoFiltrado() throws Exception {
        salvar(LocalDate.of(2026, 3, 10), "Banda A", "1000.00", true);
        salvar(LocalDate.of(2026, 3, 20), "Banda A", "500.00", false);
        salvar(LocalDate.of(2026, 4, 5), "Banda B", "300.00", true);

        mvc.perform(get("/dashboard").param("mes", "3").param("ano", "2026"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard/index"))
                .andExpect(model().attribute("resumo", is(new ResumoShows(
                        pagamento(2, 1, 1, "1500.00", "1000.00", "500.00", "750.00", "1000.00", "500.00"),
                        zerado(),
                        2,
                        moeda("1500.00")))))
                .andExpect(content().string(containsString("Dashboard")))
                .andExpect(content().string(containsString("Pagamento à vista")))
                .andExpect(content().string(containsString("Pagamento mensal")))
                .andExpect(content().string(containsString("Todos os shows")))
                .andExpect(content().string(containsString("Menor cachê")))
                .andExpect(content().string(containsString("Cachê total")))
                .andExpect(content().string(containsString("Shows no período")))
                .andExpect(content().string(not(containsString("(pagamento à vista)"))))
                .andExpect(content().string(not(containsString("(pagamento mensal)"))))
                .andExpect(content().string(containsString("Banda A")))
                .andExpect(content().string(not(containsString("<td>Banda B</td>"))));
    }

    @Test
    @WithMockUser
    void exibeRankingDeArtistasDoPeriodo() throws Exception {
        salvar(LocalDate.of(2026, 3, 10), "Banda A", "1000.00", true);
        salvar(LocalDate.of(2026, 3, 20), "Banda A", "500.00", false);
        salvar(LocalDate.of(2026, 3, 25), "Banda B", "800.00", true);

        mvc.perform(get("/dashboard").param("mes", "3").param("ano", "2026"))
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

        mvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("filtro", hasProperty("mes", is(hoje.getMonthValue()))))
                .andExpect(model().attribute("filtro", hasProperty("ano", is(hoje.getYear()))))
                .andExpect(model().attribute("resumo", is(new ResumoShows(
                        pagamento(1, 1, 0, "500.00", "500.00", "0.00", "500.00", "500.00", "500.00"),
                        zerado(),
                        1,
                        moeda("500.00")))))
                .andExpect(content().string(containsString("Banda Atual")))
                .andExpect(content().string(not(containsString("Banda Antiga"))));
    }

    @Test
    @WithMockUser
    void filtraApenasPorMesEmQualquerAno() throws Exception {
        salvar(LocalDate.of(2025, 3, 10), "Banda 2025", "100.00", true);
        salvar(LocalDate.of(2026, 3, 10), "Banda 2026", "200.00", true);
        salvar(LocalDate.of(2026, 4, 10), "Banda Abril", "300.00", true);

        mvc.perform(get("/dashboard").param("mes", "3"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("resumo", is(new ResumoShows(
                        pagamento(2, 2, 0, "300.00", "300.00", "0.00", "150.00", "200.00", "100.00"),
                        zerado(),
                        2,
                        moeda("300.00")))));
    }

    @Test
    @WithMockUser
    void filtraAnoInteiroQuandoMesNaoInformado() throws Exception {
        salvar(LocalDate.of(2025, 12, 10), "Banda 2025", "100.00", true);
        salvar(LocalDate.of(2026, 3, 10), "Banda Março", "200.00", true);
        salvar(LocalDate.of(2026, 4, 10), "Banda Abril", "300.00", true);

        mvc.perform(get("/dashboard").param("ano", "2026"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("resumo", is(new ResumoShows(
                        pagamento(2, 2, 0, "500.00", "500.00", "0.00", "250.00", "300.00", "200.00"),
                        zerado(),
                        2,
                        moeda("500.00")))));
    }

    @Test
    @WithMockUser
    void exibeGraficosDoAnoSelecionado() throws Exception {
        salvar(LocalDate.of(2026, 3, 10), "Banda A", "1000.00", true);
        salvar(LocalDate.of(2026, 3, 20), "Banda A", "500.00", false);
        salvar(LocalDate.of(2026, 4, 5), "Banda B", "300.00", true);

        List<Long> showsPorMes = listaMeses(0L);
        showsPorMes.set(2, 2L);
        showsPorMes.set(3, 1L);
        List<BigDecimal> cacheMedioPorMes = listaMeses(moeda("0.00"));
        cacheMedioPorMes.set(2, moeda("750.00"));
        cacheMedioPorMes.set(3, moeda("300.00"));

        mvc.perform(get("/dashboard").param("ano", "2026"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("graficos", is(new GraficosDashboard(
                        2026, showsPorMes, cacheMedioPorMes,
                        List.of("Banda A", "Banda B"), List.of(2L, 1L),
                        List.of(2026), List.of(3L), List.of(moeda("600.00"))))))
                .andExpect(content().string(containsString("Total de shows por mês")))
                .andExpect(content().string(containsString("Valor médio de cachê por show / mês")))
                .andExpect(content().string(containsString("Artistas atendidos no ano de")))
                .andExpect(content().string(containsString("Total de shows por ano")))
                .andExpect(content().string(containsString("Média de cachês por ano")));
    }

    @Test
    @WithMockUser
    void usaAnoCorrenteNosGraficosQuandoAnoNaoInformado() throws Exception {
        LocalDate hoje = LocalDate.now();
        salvar(hoje, "Banda Atual", "500.00", true);
        salvar(hoje.minusYears(1), "Banda Antiga", "900.00", true);

        List<Long> showsPorMes = listaMeses(0L);
        showsPorMes.set(hoje.getMonthValue() - 1, 1L);
        List<BigDecimal> cacheMedioPorMes = listaMeses(moeda("0.00"));
        cacheMedioPorMes.set(hoje.getMonthValue() - 1, moeda("500.00"));

        mvc.perform(get("/dashboard").param("mes", String.valueOf(hoje.getMonthValue())))
                .andExpect(status().isOk())
                .andExpect(model().attribute("graficos", is(new GraficosDashboard(
                        hoje.getYear(), showsPorMes, cacheMedioPorMes,
                        List.of("Banda Atual"), List.of(1L),
                        List.of(hoje.getYear() - 1, hoje.getYear()), List.of(1L, 1L),
                        List.of(moeda("900.00"), moeda("500.00"))))));
    }

    @Test
    @WithMockUser
    void exibeEstadoVazioQuandoNaoHaShowsNoPeriodo() throws Exception {
        salvar(LocalDate.of(2026, 4, 5), "Banda B", "300.00", true);

        mvc.perform(get("/dashboard").param("mes", "3").param("ano", "2026"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("resumo", is(new ResumoShows(
                        zerado(),
                        zerado(),
                        0,
                        moeda("0.00")))))
                .andExpect(model().attribute("ranking", is(List.of())))
                .andExpect(content().string(containsString("Nenhum show encontrado no período.")));
    }

    @Test
    @WithMockUser
    void consideraShowsSemCacheComoZeroNosTotais() throws Exception {
        salvar(LocalDate.of(2026, 3, 10), "Banda Sem Cachê", null, false);

        mvc.perform(get("/dashboard").param("mes", "3").param("ano", "2026"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("resumo", is(new ResumoShows(
                        pagamento(1, 0, 1, "0.00", "0.00", "0.00", "0.00", "0.00", "0.00"),
                        zerado(),
                        1,
                        moeda("0.00")))))
                .andExpect(model().attribute("ranking", is(List.of(
                        new ArtistaRanking("Banda Sem Cachê", 1, new BigDecimal("0.00"))))));
    }

    @Test
    @WithMockUser
    void ignoraShowsMensaisNosTotaisGerais() throws Exception {
        Show mensal = salvar(LocalDate.of(2026, 3, 10), "Banda Mensal", "1000.00", false);
        mensal.setPagamentoMensal(true);
        showRepository.save(mensal);
        salvar(LocalDate.of(2026, 3, 15), "Banda Avulsa", "500.00", true);

        mvc.perform(get("/dashboard").param("mes", "3").param("ano", "2026"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("resumo", is(new ResumoShows(
                        pagamento(1, 1, 0, "500.00", "500.00", "0.00", "500.00", "500.00", "500.00"),
                        pagamento(1, 0, 1, "1000.00", "0.00", "1000.00", "1000.00", "1000.00", "1000.00"),
                        2,
                        moeda("1500.00")))))
                .andExpect(content().string(containsString("Pagamento mensal")));
    }

    @Test
    @WithMockUser
    void listaProximosShowsDoPeriodoIgnorandoDatasPassadas() throws Exception {
        LocalDate futuro = LocalDate.now().plusDays(1);
        Show mensal = salvar(futuro, "Banda Futura", "800.00", false);
        mensal.setPagamentoMensal(true);
        mensal.setObservacoes("Show especial");
        showRepository.save(mensal);
        salvar(LocalDate.of(2020, 3, 10), "Banda Antiga", "100.00", true);

        mvc.perform(get("/dashboard")
                        .param("mes", String.valueOf(futuro.getMonthValue()))
                        .param("ano", String.valueOf(futuro.getYear())))
                .andExpect(status().isOk())
                .andExpect(model().attribute("proximos", contains(hasProperty("artista", is("Banda Futura")))))
                .andExpect(content().string(containsString("Próximos shows")))
                .andExpect(content().string(containsString("Banda Futura")))
                .andExpect(content().string(containsString("Show especial")))
                .andExpect(content().string(containsString(">Mensal<")))
                .andExpect(content().string(not(containsString("Banda Antiga"))));
    }

    @Test
    @WithMockUser
    void exibeMensagemQuandoNaoHaProximosShowsNoPeriodo() throws Exception {
        salvar(LocalDate.of(2020, 3, 10), "Banda Antiga", "100.00", true);

        mvc.perform(get("/dashboard").param("mes", "3").param("ano", "2020"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("proximos", is(List.of())))
                .andExpect(content().string(containsString("Nenhum show futuro no período.")));
    }

    private static ResumoPagamento pagamento(long quantidade, long pagas, long pendentes,
                                             String cacheTotal, String cachePago, String cachePendente,
                                             String cacheMedio, String maiorCache, String menorCache) {
        return new ResumoPagamento(quantidade, pagas, pendentes,
                moeda(cacheTotal), moeda(cachePago), moeda(cachePendente),
                moeda(cacheMedio), moeda(maiorCache), moeda(menorCache));
    }

    private static ResumoPagamento zerado() {
        return pagamento(0, 0, 0, "0.00", "0.00", "0.00", "0.00", "0.00", "0.00");
    }

    private static BigDecimal moeda(String valor) {
        return new BigDecimal(valor);
    }

    private static <T> List<T> listaMeses(T valorInicial) {
        return new ArrayList<>(Collections.nCopies(12, valorInicial));
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
