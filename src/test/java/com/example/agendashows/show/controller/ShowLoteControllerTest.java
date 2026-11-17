package com.example.agendashows.show.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.example.agendashows.show.entity.Show;
import com.example.agendashows.show.repository.ShowRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ShowLoteControllerTest {

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
        mvc.perform(get("/shows/lote"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/entrar"));
    }

    @Test
    @WithMockUser
    void exibeFormularioComMesEAnoSelecionados() throws Exception {
        mvc.perform(get("/shows/lote").param("mes", "3").param("ano", "2026"))
                .andExpect(status().isOk())
                .andExpect(view().name("shows/lote"))
                .andExpect(model().attribute("formulario", hasProperty("mes", is(3))))
                .andExpect(model().attribute("formulario", hasProperty("ano", is(2026))))
                .andExpect(content().string(containsString("Segunda-feira")))
                .andExpect(content().string(containsString("Pagamento mensal")));
    }

    @Test
    @WithMockUser
    void geraUmShowParaCadaSegundaFeiraDoMes() throws Exception {
        mvc.perform(post("/shows/lote").with(csrf())
                        .param("mes", "3")
                        .param("ano", "2026")
                        .param("segunda", "true")
                        .param("horario", "21:30")
                        .param("cache", "1500.50")
                        .param("artista", "Banda X")
                        .param("local", "Casa de Show")
                        .param("observacoes", "Levar equipamento")
                        .param("pagamentoMensal", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/shows?mes=3&ano=2026"))
                .andExpect(flash().attribute("mensagem", "5 shows cadastrados com sucesso."));

        List<Show> shows = showRepository.findAll();
        assertThat(shows).hasSize(5);
        assertThat(shows).extracting(Show::getData).containsExactly(
                LocalDate.of(2026, 3, 2),
                LocalDate.of(2026, 3, 9),
                LocalDate.of(2026, 3, 16),
                LocalDate.of(2026, 3, 23),
                LocalDate.of(2026, 3, 30));
        assertThat(shows).allSatisfy(show -> {
            assertThat(show.getData().getDayOfWeek().getValue()).isEqualTo(1);
            assertThat(show.getHorario()).isEqualTo(LocalTime.of(21, 30));
            assertThat(show.getCache()).isEqualByComparingTo(new BigDecimal("1500.50"));
            assertThat(show.getArtista()).isEqualTo("Banda X");
            assertThat(show.getLocal()).isEqualTo("Casa de Show");
            assertThat(show.getObservacoes()).isEqualTo("Levar equipamento");
            assertThat(show.isPagamentoMensal()).isTrue();
            assertThat(show.isPago()).isFalse();
        });
    }

    @Test
    @WithMockUser
    void combinaVariosDiasDaSemanaSemDuplicarDatas() throws Exception {
        mvc.perform(post("/shows/lote").with(csrf())
                        .param("mes", "3")
                        .param("ano", "2026")
                        .param("segunda", "true")
                        .param("quarta", "true")
                        .param("artista", "Banda Y")
                        .param("local", "Praça"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("mensagem", "9 shows cadastrados com sucesso."));

        assertThat(showRepository.count()).isEqualTo(9);
    }

    @Test
    @WithMockUser
    void geraUmShowParaCadaDomingoDoMes() throws Exception {
        mvc.perform(post("/shows/lote").with(csrf())
                        .param("mes", "2")
                        .param("ano", "2026")
                        .param("domingo", "true")
                        .param("artista", "Banda Z")
                        .param("local", "Bar"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute("mensagem", "4 shows cadastrados com sucesso."));

        assertThat(showRepository.findAll()).extracting(Show::getData).containsExactly(
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2026, 2, 8),
                LocalDate.of(2026, 2, 15),
                LocalDate.of(2026, 2, 22));
    }

    @Test
    @WithMockUser
    void rejeitaLoteSemDiaDaSemanaSelecionado() throws Exception {
        mvc.perform(post("/shows/lote").with(csrf())
                        .param("mes", "3")
                        .param("ano", "2026")
                        .param("artista", "Banda")
                        .param("local", "Local"))
                .andExpect(status().isOk())
                .andExpect(view().name("shows/lote"))
                .andExpect(model().attributeHasFieldErrors("formulario", "aoMenosUmDiaSelecionado"))
                .andExpect(content().string(containsString("Selecione ao menos um dia da semana.")));

        assertThat(showRepository.count()).isZero();
    }

    @Test
    @WithMockUser
    void exibeErrosQuandoMesAnoArtistaELocalInvalidos() throws Exception {
        mvc.perform(post("/shows/lote").with(csrf())
                        .param("segunda", "true")
                        .param("artista", "")
                        .param("local", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("shows/lote"))
                .andExpect(model().attributeHasFieldErrors("formulario", "mes", "ano", "artista", "local"));

        assertThat(showRepository.count()).isZero();
    }

    @Test
    @WithMockUser
    void rejeitaPostSemCsrf() throws Exception {
        MockHttpServletRequestBuilder requisicao = post("/shows/lote")
                .param("mes", "3")
                .param("ano", "2026")
                .param("segunda", "true")
                .param("artista", "Banda")
                .param("local", "Local");

        mvc.perform(requisicao).andExpect(status().isForbidden());
        assertThat(showRepository.count()).isZero();
    }
}
