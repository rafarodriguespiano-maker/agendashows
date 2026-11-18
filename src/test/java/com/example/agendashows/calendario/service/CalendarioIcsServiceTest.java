package com.example.agendashows.calendario.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.agendashows.show.entity.Show;
import com.example.agendashows.show.repository.ShowRepository;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CalendarioIcsServiceTest {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    @Mock
    private ShowRepository showRepository;

    private CalendarioIcsService service;

    @BeforeEach
    void configurar() {
        service = new CalendarioIcsService(showRepository, FUSO.getId(), 180);
    }

    @Test
    void geraEventoComHorarioConvertidoParaUtc() {
        Show show = show(1L, LocalDate.of(2026, 12, 1), LocalTime.of(22, 0), "Banda X", "Casa Y", "Observação");
        when(showRepository.buscarProximos(eq(LocalDate.now(FUSO)), isNull(), isNull(), isNull()))
                .thenReturn(List.of(show));

        String ics = service.gerarFeed();

        assertThat(ics)
                .startsWith("BEGIN:VCALENDAR\r\n")
                .contains("BEGIN:VEVENT\r\n")
                .contains("UID:show-1@agendashows\r\n")
                .contains("DTSTART:20261202T010000Z\r\n")
                .contains("DTEND:20261202T040000Z\r\n")
                .contains("SUMMARY:Banda X\r\n")
                .contains("LOCATION:Casa Y\r\n")
                .contains("DESCRIPTION:Observação\r\n")
                .endsWith("END:VCALENDAR\r\n");
    }

    @Test
    void geraEventoDeDiaInteiroQuandoNaoHaHorario() {
        Show show = show(2L, LocalDate.of(2026, 12, 15), null, "Banda Sem Hora", "Casa Y", null);
        when(showRepository.buscarProximos(eq(LocalDate.now(FUSO)), isNull(), isNull(), isNull()))
                .thenReturn(List.of(show));

        String ics = service.gerarFeed();

        assertThat(ics)
                .contains("DTSTART;VALUE=DATE:20261215\r\n")
                .contains("DTEND;VALUE=DATE:20261216\r\n")
                .doesNotContain("T010000Z");
    }

    @Test
    void escapaCaracteresEspeciaisDoIcalendar() {
        Show show = show(3L, LocalDate.of(2026, 12, 20), LocalTime.of(20, 0),
                "A; B, C\\D", "Local; com, vírgula", "Linha 1\nLinha 2");
        when(showRepository.buscarProximos(eq(LocalDate.now(FUSO)), isNull(), isNull(), isNull()))
                .thenReturn(List.of(show));

        String ics = service.gerarFeed();

        assertThat(ics)
                .contains("SUMMARY:A\\; B\\, C\\\\D\r\n")
                .contains("LOCATION:Local\\; com\\, vírgula\r\n")
                .contains("DESCRIPTION:Linha 1\\nLinha 2\r\n");
    }

    @Test
    void dobraLinhasLongasEmAte75Octetos() {
        String observacao = "Show com texto muito longo ".repeat(10);
        Show show = show(4L, LocalDate.of(2026, 12, 20), LocalTime.of(20, 0), "Banda", "Local", observacao);
        when(showRepository.buscarProximos(eq(LocalDate.now(FUSO)), isNull(), isNull(), isNull()))
                .thenReturn(List.of(show));

        String[] linhas = service.gerarFeed().split("\r\n");

        assertThat(linhas).allSatisfy(linha ->
                assertThat(linha.getBytes(StandardCharsets.UTF_8).length).isLessThanOrEqualTo(75));
        assertThat(service.gerarFeed()).contains("\r\n ");
    }

    @Test
    void consultaSomenteShowsAPartirDeHoje() {
        when(showRepository.buscarProximos(eq(LocalDate.now(FUSO)), isNull(), isNull(), isNull()))
                .thenReturn(List.of());

        service.gerarFeed();

        verify(showRepository).buscarProximos(eq(LocalDate.now(FUSO)), isNull(), isNull(), isNull());
    }

    private Show show(Long id, LocalDate data, LocalTime horario, String artista, String local, String observacoes) {
        Show show = new Show();
        show.setId(id);
        show.setData(data);
        show.setHorario(horario);
        show.setArtista(artista);
        show.setLocal(local);
        show.setObservacoes(observacoes);
        return show;
    }
}
