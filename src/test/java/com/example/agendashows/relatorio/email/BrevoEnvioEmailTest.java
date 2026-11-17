package com.example.agendashows.relatorio.email;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class BrevoEnvioEmailTest {

    private static final String URL = "https://api.brevo.com/v3/smtp/email";

    private MockRestServiceServer server;
    private BrevoEnvioEmail envioEmail;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        envioEmail = new BrevoEnvioEmail(builder, "chave-teste", URL,
                "nao-responda@exemplo.com", "Agenda de Shows");
    }

    @Test
    void enviaPayloadParaApiDoBrevo() {
        server.expect(requestTo(URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("api-key", "chave-teste"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().string(containsString("\"email\":\"musico@exemplo.com\"")))
                .andExpect(content().string(containsString("\"email\":\"nao-responda@exemplo.com\"")))
                .andExpect(content().string(containsString("\"name\":\"Agenda de Shows\"")))
                .andExpect(content().string(containsString("\"subject\":\"Shows da semana\"")))
                .andExpect(content().string(containsString("\"htmlContent\":\"<html>ok</html>\"")))
                .andExpect(content().string(containsString("\"textContent\":\"ok\"")))
                .andRespond(withSuccess());

        envioEmail.enviar(new EmailMensagem("musico@exemplo.com", "Shows da semana", "<html>ok</html>", "ok"));

        server.verify();
    }

    @Test
    void converteErroDeApiEmEnvioEmailException() {
        server.expect(requestTo(URL))
                .andRespond(withBadRequest()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"message\":\"Sender not valid\"}"));

        assertThatThrownBy(() -> envioEmail.enviar(
                new EmailMensagem("musico@exemplo.com", "Shows da semana", "<html>ok</html>", "ok")))
                .isInstanceOf(EnvioEmailException.class)
                .hasMessageContaining("HTTP 400")
                .hasMessageContaining("Sender not valid");
    }

    @Test
    void converteChaveInvalidaEmEnvioEmailException() {
        server.expect(requestTo(URL))
                .andRespond(withUnauthorizedRequest().body("{\"message\":\"Key not found\"}"));

        assertThatThrownBy(() -> envioEmail.enviar(
                new EmailMensagem("musico@exemplo.com", "Shows da semana", "<html>ok</html>", "ok")))
                .isInstanceOf(EnvioEmailException.class)
                .hasMessageContaining("HTTP 401");
    }
}
