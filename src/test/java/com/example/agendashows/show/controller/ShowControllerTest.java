package com.example.agendashows.show.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import com.example.agendashows.show.entity.Show;
import com.example.agendashows.show.repository.ShowRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
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
class ShowControllerTest {

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
        mvc.perform(get("/shows"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/entrar"));
    }

    @Test
    @WithMockUser
    void listaShowsAplicandoFiltros() throws Exception {
        salvar(LocalDate.of(2026, 3, 10), "Banda Visível", true);
        salvar(LocalDate.of(2026, 5, 10), "Banda Oculta", true);

        mvc.perform(get("/shows").param("mes", "3").param("ano", "2026").param("situacaoPagamento", "PAGO"))
                .andExpect(status().isOk())
                .andExpect(view().name("shows/list"))
                .andExpect(content().string(containsString("/shows/novo?mes=3&amp;ano=2026")))
                .andExpect(content().string(containsString("Banda Visível")))
                .andExpect(content().string(not(containsString("Banda Oculta"))));
    }

    @Test
    @WithMockUser
    void mantemMesEAnoNoRedirecionamentoAoCriarShow() throws Exception {
        mvc.perform(post("/shows").with(csrf())
                        .param("data", "2026-03-15")
                        .param("artista", "Banda Nova")
                        .param("local", "Casa de Show")
                        .param("mes", "3")
                        .param("ano", "2026"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/shows?mes=3&ano=2026"));

        salvar(LocalDate.of(2026, 3, 10), "Banda Visível", true);
        salvar(LocalDate.of(2026, 5, 10), "Banda Oculta", true);

        mvc.perform(get("/shows").param("mes", "3").param("ano", "2026"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("filtro", hasProperty("mes", is(3))))
                .andExpect(content().string(containsString("Banda Visível")))
                .andExpect(content().string(containsString("Banda Nova")))
                .andExpect(content().string(not(containsString("Banda Oculta"))));
    }

    @Test
    @WithMockUser
    void mantemMesEAnoAoExibirErrosDeValidacao() throws Exception {
        mvc.perform(post("/shows").with(csrf())
                        .param("artista", "")
                        .param("local", "")
                        .param("mes", "3")
                        .param("ano", "2026"))
                .andExpect(status().isOk())
                .andExpect(view().name("shows/form"))
                .andExpect(model().attribute("mes", is(3)))
                .andExpect(model().attribute("ano", is(2026)));
    }

    @Test
    @WithMockUser
    void carregaFiltroNosLinksDoFormulario() throws Exception {
        mvc.perform(get("/shows/novo").param("mes", "3").param("ano", "2026"))
                .andExpect(status().isOk())
                .andExpect(view().name("shows/form"))
                .andExpect(model().attribute("mes", is(3)))
                .andExpect(model().attribute("ano", is(2026)))
                .andExpect(content().string(containsString("name=\"mes\" value=\"3\"")))
                .andExpect(content().string(containsString("name=\"ano\" value=\"2026\"")));
    }

    @Test
    @WithMockUser
    void mantemMesEAnoNoRedirecionamentoAoAtualizarShow() throws Exception {
        Show show = salvar(LocalDate.of(2026, 3, 10), "Banda", false);

        mvc.perform(post("/shows/{id}", show.getId()).with(csrf())
                        .param("data", "2026-03-10")
                        .param("artista", "Banda Editada")
                        .param("local", "Local")
                        .param("mes", "3")
                        .param("ano", "2026"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/shows?mes=3&ano=2026"));
    }

    @Test
    @WithMockUser
    void criaShowComDadosValidos() throws Exception {
        mvc.perform(post("/shows").with(csrf())
                        .param("data", "2026-05-10")
                        .param("artista", "Banda X")
                        .param("local", "Casa de Show")
                        .param("horario", "21:00")
                        .param("cache", "1500.50")
                        .param("pago", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/shows"));

        assertThat(showRepository.count()).isEqualTo(1);
        Show show = showRepository.findAll().getFirst();
        assertThat(show.getArtista()).isEqualTo("Banda X");
        assertThat(show.getCache()).isEqualByComparingTo("1500.50");
        assertThat(show.isPago()).isTrue();
    }

    @Test
    @WithMockUser
    void aceitaShowSemHorarioNemCache() throws Exception {
        mvc.perform(post("/shows").with(csrf())
                        .param("data", "2026-05-10")
                        .param("artista", "Banda Y")
                        .param("local", "Praça"))
                .andExpect(status().is3xxRedirection());

        Show show = showRepository.findAll().getFirst();
        assertThat(show.getHorario()).isNull();
        assertThat(show.getCache()).isNull();
    }

    @Test
    @WithMockUser
    void exibeErrosDeValidacaoNoFormulario() throws Exception {
        mvc.perform(post("/shows").with(csrf())
                        .param("artista", "")
                        .param("local", "")
                        .param("cache", "-10"))
                .andExpect(status().isOk())
                .andExpect(view().name("shows/form"))
                .andExpect(model().attributeHasFieldErrors("formulario", "data", "artista", "local", "cache"));

        assertThat(showRepository.count()).isZero();
    }

    @Test
    @WithMockUser
    void alternaPagamento() throws Exception {
        Show show = salvar(LocalDate.of(2026, 3, 10), "Banda", false);

        mvc.perform(post("/shows/{id}/pagamento", show.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/shows"));

        assertThat(showRepository.findById(show.getId())).get().extracting(Show::isPago).isEqualTo(true);
    }

    @Test
    @WithMockUser
    void excluiShow() throws Exception {
        Show show = salvar(LocalDate.of(2026, 3, 10), "Banda", false);

        mvc.perform(post("/shows/{id}/excluir", show.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection());

        assertThat(showRepository.count()).isZero();
    }

    @Test
    @WithMockUser
    void showInexistenteRedirecionaComErro() throws Exception {
        mvc.perform(get("/shows/{id}/editar", 9999L))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/shows"));
    }

    @Test
    @WithMockUser
    void rejeitaPostSemCsrf() throws Exception {
        mvc.perform(post("/shows")
                        .param("data", "2026-05-10")
                        .param("artista", "Banda")
                        .param("local", "Local"))
                .andExpect(status().isForbidden());
    }

    private Show salvar(LocalDate data, String artista, boolean pago) {
        Show show = new Show();
        show.setData(data);
        show.setArtista(artista);
        show.setLocal("Local");
        show.setCache(new BigDecimal("500.00"));
        show.setPago(pago);
        return showRepository.save(show);
    }
}
