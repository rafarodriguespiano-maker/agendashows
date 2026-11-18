package com.example.agendashows.dashboard.service;

import com.example.agendashows.dashboard.dto.ArtistaRanking;
import com.example.agendashows.dashboard.dto.GraficosDashboard;
import com.example.agendashows.dashboard.dto.PeriodoFilter;
import com.example.agendashows.dashboard.dto.ResumoPagamento;
import com.example.agendashows.dashboard.dto.ResumoShows;
import com.example.agendashows.show.entity.Show;
import com.example.agendashows.show.repository.ShowRepository;
import com.example.agendashows.show.repository.ShowRepository.ArtistaParticipacaoProjection;
import com.example.agendashows.show.repository.ShowRepository.ResumoAnoProjection;
import com.example.agendashows.show.repository.ShowRepository.ResumoMesProjection;
import com.example.agendashows.show.repository.ShowRepository.ResumoPeriodoProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int LIMITE_RANKING = 10;
    private static final int ESCALA_MONETARIA = 2;

    private final ShowRepository showRepository;

    @Transactional(readOnly = true)
    public ResumoShows resumir(PeriodoFilter filtro) {
        Periodo periodo = Periodo.de(filtro);
        ResumoPeriodoProjection resumo = showRepository.resumirPeriodo(periodo.inicio(), periodo.fim(), periodo.mes());

        return new ResumoShows(
                new ResumoPagamento(
                        numero(resumo.getQuantidadeShows()),
                        numero(resumo.getQuantidadePagas()),
                        numero(resumo.getQuantidadePendentes()),
                        moeda(resumo.getCacheTotal()),
                        moeda(resumo.getCachePago()),
                        moeda(resumo.getCachePendente()),
                        moeda(resumo.getCacheMedio()),
                        moeda(resumo.getMaiorCache()),
                        moeda(resumo.getMenorCache())),
                new ResumoPagamento(
                        numero(resumo.getQuantidadeMensais()),
                        numero(resumo.getQuantidadeMensaisPagas()),
                        numero(resumo.getQuantidadeMensaisPendentes()),
                        moeda(resumo.getCacheMensal()),
                        moeda(resumo.getCacheMensalPago()),
                        moeda(resumo.getCacheMensalPendente()),
                        moeda(resumo.getCacheMensalMedio()),
                        moeda(resumo.getMaiorCacheMensal()),
                        moeda(resumo.getMenorCacheMensal())),
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

    /**
     * Dados dos gráficos do ano selecionado no filtro. Sem ano informado, usa o ano corrente.
     *
     * <p>O mês do filtro não é considerado: os gráficos sempre comparam os doze meses do ano.</p>
     */
    @Transactional(readOnly = true)
    public GraficosDashboard graficos(PeriodoFilter filtro) {
        Integer anoFiltrado = filtro.anoValido();
        int ano = anoFiltrado == null ? LocalDate.now().getYear() : anoFiltrado;
        LocalDate inicio = LocalDate.of(ano, 1, 1);
        LocalDate fim = inicio.plusYears(1);

        Map<Integer, ResumoMesProjection> resumosPorMes = showRepository.resumoPorMes(inicio, fim)
                .stream()
                .collect(Collectors.toMap(ResumoMesProjection::getMes, Function.identity()));

        List<Long> showsPorMes = new ArrayList<>(12);
        List<BigDecimal> cacheMedioPorMes = new ArrayList<>(12);
        for (int mes = 1; mes <= 12; mes++) {
            ResumoMesProjection resumo = resumosPorMes.get(mes);
            showsPorMes.add(resumo == null ? 0L : numero(resumo.getQuantidadeShows()));
            cacheMedioPorMes.add(resumo == null ? moeda(BigDecimal.ZERO) : moeda(resumo.getCacheMedio()));
        }

        List<ArtistaParticipacaoProjection> participacoes = showRepository.participacaoDeArtistas(inicio, fim);

        List<ResumoAnoProjection> resumosPorAno = showRepository.resumoPorAno();

        return new GraficosDashboard(
                ano,
                showsPorMes,
                cacheMedioPorMes,
                participacoes.stream().map(ArtistaParticipacaoProjection::getArtista).toList(),
                participacoes.stream().map(item -> numero(item.getQuantidadeShows())).toList(),
                resumosPorAno.stream().map(ResumoAnoProjection::getAno).toList(),
                resumosPorAno.stream().map(item -> numero(item.getQuantidadeShows())).toList(),
                resumosPorAno.stream().map(item -> moeda(item.getCacheMedio())).toList());
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
