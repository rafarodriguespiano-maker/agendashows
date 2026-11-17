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
     * <p>Os totais gerais consideram apenas shows sem pagamento mensal; os campos de pagamento mensal
     * ({@code quantidadeMensais} e {@code cacheMensal}) consideram apenas os marcados como mensais.</p>
     */
    @Query("""
            select sum(case when s.pagamentoMensal = false then 1 else 0 end) as quantidadeShows,
                   sum(case when s.pagamentoMensal = false then s.cache end) as cacheTotal,
                   sum(case when s.pagamentoMensal = false and s.pago = true then s.cache end) as cachePago,
                   sum(case when s.pagamentoMensal = false and s.pago = false then s.cache end) as cachePendente,
                   sum(case when s.pagamentoMensal = false and s.pago = true then 1 else 0 end) as quantidadePagas,
                   sum(case when s.pagamentoMensal = false and s.pago = false then 1 else 0 end) as quantidadePendentes,
                   sum(case when s.pagamentoMensal = true then 1 else 0 end) as quantidadeMensais,
                   sum(case when s.pagamentoMensal = true then s.cache end) as cacheMensal,
                   avg(case when s.pagamentoMensal = false then s.cache end) as cacheMedio,
                   max(case when s.pagamentoMensal = false then s.cache end) as maiorCache,
                   min(case when s.pagamentoMensal = false then s.cache end) as menorCache
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

        BigDecimal getCacheMensal();

        Double getCacheMedio();

        BigDecimal getMaiorCache();

        BigDecimal getMenorCache();
    }

    interface ArtistaRankingProjection {

        String getArtista();

        Long getQuantidadeShows();

        BigDecimal getCacheTotal();
    }
}
