package com.example.agendashows.estatisticas.service;

import com.example.agendashows.estatisticas.dto.ArtistaRanking;
import com.example.agendashows.estatisticas.dto.PeriodoFilter;
import com.example.agendashows.estatisticas.dto.ResumoShows;
import com.example.agendashows.show.entity.Show;
import com.example.agendashows.show.repository.ShowRepository;
import com.example.agendashows.show.repository.ShowRepository.ResumoPeriodoProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EstatisticasService {

    private static final int LIMITE_RANKING = 10;
    private static final int ESCALA_MONETARIA = 2;

    private final ShowRepository showRepository;

    @Transactional(readOnly = true)
    public ResumoShows resumir(PeriodoFilter filtro) {
        Periodo periodo = Periodo.de(filtro);
        ResumoPeriodoProjection resumo = showRepository.resumirPeriodo(periodo.inicio(), periodo.fim(), periodo.mes());

        return new ResumoShows(
                numero(resumo.getQuantidadeShows()),
                moeda(resumo.getCacheTotal()),
                moeda(resumo.getCachePago()),
                moeda(resumo.getCachePendente()),
                numero(resumo.getQuantidadePagas()),
                numero(resumo.getQuantidadePendentes()),
                numero(resumo.getQuantidadeMensais()),
                moeda(resumo.getCacheMensal()),
                moeda(resumo.getCacheMedio()),
                moeda(resumo.getMaiorCache()),
                moeda(resumo.getMenorCache()),
                numero(resumo.getQuantidadeShowsGeral()),
                moeda(resumo.getCacheGeral()));
    }

    @Transactional(readOnly = true)
    public List<ArtistaRanking> ranking(PeriodoFilter filtro) {
        Periodo periodo = Periodo.de(filtro);

        return showRepository
                .rankingDeArtistas(periodo.inicio(), periodo.fim(), periodo.mes(), PageRequest.of(0, LIMITE_RANKING))
                .stream()
                .map(item -> new ArtistaRanking(
                        item.getArtista(),
                        numero(item.getQuantidadeShows()),
                        moeda(item.getCacheTotal())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Show> proximosShows(PeriodoFilter filtro) {
        Periodo periodo = Periodo.de(filtro);
        return showRepository.buscarProximos(LocalDate.now(), periodo.inicio(), periodo.fim(), periodo.mes());
    }

    private static long numero(Long valor) {
        return valor == null ? 0L : valor;
    }

    private static BigDecimal moeda(BigDecimal valor) {
        return valor == null
                ? BigDecimal.ZERO.setScale(ESCALA_MONETARIA)
                : valor.setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP);
    }

    private static BigDecimal moeda(Double valor) {
        return valor == null
                ? BigDecimal.ZERO.setScale(ESCALA_MONETARIA)
                : BigDecimal.valueOf(valor).setScale(ESCALA_MONETARIA, RoundingMode.HALF_UP);
    }
}
