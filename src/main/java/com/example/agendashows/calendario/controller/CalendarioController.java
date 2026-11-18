package com.example.agendashows.calendario.controller;

import com.example.agendashows.calendario.service.CalendarioIcsService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Feed iCalendar público (protegido por token na URL), consumido pelo Apple Calendar na assinatura de calendário.
 */
@Controller
@RequestMapping("/calendario")
public class CalendarioController {

    private static final MediaType TIPO_CALENDARIO =
            new MediaType("text", "calendar", StandardCharsets.UTF_8);

    private final CalendarioIcsService calendarioIcsService;

    @Value("${app.calendario.token:}")
    private String token;

    public CalendarioController(CalendarioIcsService calendarioIcsService) {
        this.calendarioIcsService = calendarioIcsService;
    }

    @GetMapping("/{tokenArquivo}.ics")
    public ResponseEntity<String> feed(@PathVariable String tokenArquivo,
                                       @RequestParam(name = "download", required = false) String download) {
        if (!tokenValido(tokenArquivo)) {
            return ResponseEntity.notFound().build();
        }

        HttpHeaders cabecalhos = new HttpHeaders();
        cabecalhos.setContentType(TIPO_CALENDARIO);
        cabecalhos.setCacheControl("no-cache");
        if (download != null) {
            cabecalhos.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"shows.ics\"");
        }

        return new ResponseEntity<>(calendarioIcsService.gerarFeed(), cabecalhos, HttpStatus.OK);
    }

    private boolean tokenValido(String recebido) {
        if (!StringUtils.hasText(token) || recebido == null) {
            return false;
        }
        return MessageDigest.isEqual(
                token.getBytes(StandardCharsets.UTF_8),
                recebido.getBytes(StandardCharsets.UTF_8));
    }
}
