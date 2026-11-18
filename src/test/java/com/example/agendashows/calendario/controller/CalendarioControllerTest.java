package com.example.agendashows.calendario.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.agendashows.show.entity.Show;
import com.example.agendashows.show.repository.ShowRepository;
import java.time.LocalDate;
import java.time.LocalTime;
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
class CalendarioControllerTest {

    private static final String TOKEN = "token-de-teste";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ShowRepository showRepository;

    @BeforeEach
    void limparBanco() {
        showRepository.deleteAll();
    }

    @Test
    void entregaFeedSemExigirAutenticacao() throws Exception {
        salvar(LocalDate.now().plusDays(2), "Banda Futura", "Casa de Show");

        mvc.perform(get("/calendario/" + TOKEN + ".ics"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/calendar"))
                .andExpect(content().string(containsString("BEGIN:VCALENDAR")))
                .andExpect(content().string(containsString("SUMMARY:Banda Futura")));
    }

    @Test
    void ignoraShowsJaOcorridos() throws Exception {
        salvar(LocalDate.now().minusDays(2), "Banda Passada", "Casa Antiga");
        salvar(LocalDate.now().plusDays(2), "Banda Futura", "Casa Nova");

        mvc.perform(get("/calendario/" + TOKEN + ".ics"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Banda Futura")))
                .andExpect(content().string(not(containsString("Banda Passada"))));
    }

    @Test
    void respondeNaoEncontradoParaTokenInvalido() throws Exception {
        mvc.perform(get("/calendario/token-errado.ics"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser
    void listagemExibeLinkDeAssinatura() throws Exception {
        mvc.perform(get("/shows"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-calendario-token=\"token-de-teste\"")))
                .andExpect(content().string(containsString("/calendario/token-de-teste.ics")));
    }

    @Test
    void enviaComoAnexoQuandoSolicitadoDownload() throws Exception {
        salvar(LocalDate.now().plusDays(2), "Banda Futura", "Casa de Show");

        mvc.perform(get("/calendario/" + TOKEN + ".ics").param("download", "true"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(header().string("Content-Disposition", containsString("shows.ics")));
    }

    private void salvar(LocalDate data, String artista, String local) {
        Show show = new Show();
        show.setData(data);
        show.setHorario(LocalTime.of(22, 0));
        show.setArtista(artista);
        show.setLocal(local);
        showRepository.save(show);
    }
}
