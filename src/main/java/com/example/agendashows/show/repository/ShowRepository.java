package com.example.agendashows.show.repository;

import com.example.agendashows.show.entity.Show;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShowRepository extends JpaRepository<Show, Long>, JpaSpecificationExecutor<Show> {

    /**
     * Agrega os totais do período. Os parâmetros de período são opcionais: quando nulos, não restringem a consulta.
     *
     * <p>Os campos sem sufixo consideram os shows sem pagamento mensal (pagamento à vista);
     * os campos iniciados por {@code mensal} consideram apenas os shows com pagamento mensal.
     * Já {@code quantidadeShowsGeral} e {@code cacheGeral} consideram todos os shows do período.</p>
     */
    @Query("""
            select sum(case when s.pagamentoMensal = false then 1 else 0 end) as quantidadeShows,
                   sum(case when s.pagamentoMensal = false then s.cache end) as cacheTotal,
                   sum(case when s.pagamentoMensal = false and s.pago = true then s.cache end) as cachePago,
                   sum(case when s.pagamentoMensal = false and s.pago = false then s.cache end) as cachePendente,
                   sum(case when s.pagamentoMensal = false and s.pago = true then 1 else 0 end) as quantidadePagas,
                   sum(case when s.pagamentoMensal = false and s.pago = false then 1 else 0 end) as quantidadePendentes,
                   avg(case when s.pagamentoMensal = false then s.cache end) as cacheMedio,
                   max(case when s.pagamentoMensal = false then s.cache end) as maiorCache,
                   min(case when s.pagamentoMensal = false then s.cache end) as menorCache,
                   sum(case when s.pagamentoMensal = true then 1 else 0 end) as quantidadeMensais,
                   sum(case when s.pagamentoMensal = true and s.pago = true then 1 else 0 end) as quantidadeMensaisPagas,
                   sum(case when s.pagamentoMensal = true and s.pago = false then 1 else 0 end) as quantidadeMensaisPendentes,
                   sum(case when s.pagamentoMensal = true then s.cache end) as cacheMensal,
                   sum(case when s.pagamentoMensal = true and s.pago = true then s.cache end) as cacheMensalPago,
                   sum(case when s.pagamentoMensal = true and s.pago = false then s.cache end) as cacheMensalPendente,
                   avg(case when s.pagamentoMensal = true then s.cache end) as cacheMensalMedio,
                   max(case when s.pagamentoMensal = true then s.cache end) as maiorCacheMensal,
                   min(case when s.pagamentoMensal = true then s.cache end) as menorCacheMensal,
                   count(s) as quantidadeShowsGeral,
                   sum(s.cache) as cacheGeral
            from Show s
            where (:inicio is null or s.data >= :inicio)
              and (:fim is null or s.data < :fim)
              and (:mes is null or month(s.data) = :mes)
            """)
    ResumoPeriodoProjection resumirPeriodo(@Param("inicio") LocalDate inicio,
                                           @Param("fim") LocalDate fim,
                                           @Param("mes") Integer mes);

    /**
     * Ranking de artistas por quantidade de shows no período.
     */
    @Query("""
            select s.artista as artista,
                   count(s) as quantidadeShows,
                   sum(s.cache) as cacheTotal
            from Show s
            where (:inicio is null or s.data >= :inicio)
              and (:fim is null or s.data < :fim)
              and (:mes is null or month(s.data) = :mes)
            group by s.artista
            order by count(s) desc, sum(s.cache) desc, s.artista asc
            """)
    List<ArtistaRankingProjection> rankingDeArtistas(@Param("inicio") LocalDate inicio,
                                                     @Param("fim") LocalDate fim,
                                                     @Param("mes") Integer mes,
                                                     Pageable paginacao);

    /**
     * Próximos shows do período: considera apenas datas a partir de hoje, em ordem cronológica.
     */
    @Query("""
            select s
            from Show s
            where s.data >= :hoje
              and (:inicio is null or s.data >= :inicio)
              and (:fim is null or s.data < :fim)
              and (:mes is null or month(s.data) = :mes)
            order by s.data asc, s.horario asc
            """)
    List<Show> buscarProximos(@Param("hoje") LocalDate hoje,
                              @Param("inicio") LocalDate inicio,
                              @Param("fim") LocalDate fim,
                              @Param("mes") Integer mes);

    interface ResumoPeriodoProjection {

        Long getQuantidadeShows();

        BigDecimal getCacheTotal();

        BigDecimal getCachePago();

        BigDecimal getCachePendente();

        Long getQuantidadePagas();

        Long getQuantidadePendentes();

        Long getQuantidadeMensais();

        Long getQuantidadeMensaisPagas();

        Long getQuantidadeMensaisPendentes();

        BigDecimal getCacheMensal();

        BigDecimal getCacheMensalPago();

        BigDecimal getCacheMensalPendente();

        Double getCacheMensalMedio();

        BigDecimal getMaiorCacheMensal();

        BigDecimal getMenorCacheMensal();

        Double getCacheMedio();

        BigDecimal getMaiorCache();

        BigDecimal getMenorCache();

        Long getQuantidadeShowsGeral();

        BigDecimal getCacheGeral();
    }

    interface ArtistaRankingProjection {

        String getArtista();

        Long getQuantidadeShows();

        BigDecimal getCacheTotal();
    }
}
