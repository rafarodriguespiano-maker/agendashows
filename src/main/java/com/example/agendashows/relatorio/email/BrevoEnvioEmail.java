package com.example.agendashows.relatorio.email;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Transporte via API HTTPS do Brevo.
 *
 * <p>Usado em ambientes que bloqueiam SMTP de saída, como a Railway nos planos Free, Trial e Hobby.
 * A API usa porta 443 e não sofre esse bloqueio.</p>
 */
@Component
@Conditional(BrevoHabilitadoCondition.class)
public class BrevoEnvioEmail implements EnvioEmail {

    private final RestClient restClient;
    private final String apiKey;
    private final String remetente;
    private final String nomeRemetente;
    private final String url;

    public BrevoEnvioEmail(RestClient.Builder restClientBuilder,
                           @Value("${app.relatorio-email.brevo.api-key:}") String apiKey,
                           @Value("${app.relatorio-email.brevo.url:https://api.brevo.com/v3/smtp/email}") String url,
                           @Value("${app.relatorio-email.remetente:}") String remetente,
                           @Value("${app.relatorio-email.nome-remetente:}") String nomeRemetente) {
        this.restClient = restClientBuilder.build();
        this.apiKey = apiKey;
        this.url = url;
        this.remetente = remetente;
        this.nomeRemetente = nomeRemetente;
    }

    @Override
    public void enviar(EmailMensagem mensagem) {
        try {
            restClient.post()
                    .uri(url)
                    .header("api-key", apiKey)
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload(mensagem))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            throw new EnvioEmailException(
                    "Brevo respondeu HTTP %d para %s: %s".formatted(
                            ex.getStatusCode().value(), mensagem.destinatario(), ex.getResponseBodyAsString()),
                    ex);
        } catch (RestClientException ex) {
            throw new EnvioEmailException(
                    "Falha de comunicação com a API do Brevo para " + mensagem.destinatario(), ex);
        }
    }

    private Map<String, Object> payload(EmailMensagem mensagem) {
        Map<String, Object> remetentePayload = new LinkedHashMap<>();
        remetentePayload.put("email", remetente);
        if (StringUtils.hasText(nomeRemetente)) {
            remetentePayload.put("name", nomeRemetente);
        }

        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("sender", remetentePayload);
        corpo.put("to", List.of(Map.of("email", mensagem.destinatario())));
        corpo.put("subject", mensagem.assunto());
        corpo.put("htmlContent", mensagem.html());
        corpo.put("textContent", mensagem.texto());
        return corpo;
    }
}
