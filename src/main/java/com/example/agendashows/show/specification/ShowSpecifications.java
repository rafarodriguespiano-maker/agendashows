package com.example.agendashows.show.specification;

import com.example.agendashows.show.dto.ShowFilter;
import com.example.agendashows.show.entity.enums.StatusPagamento;
import com.example.agendashows.show.entity.Show;
import java.time.LocalDate;
import java.time.Month;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public final class ShowSpecifications {

    private ShowSpecifications() {
    }

    public static Specification<Show> comFiltros(ShowFilter filtro) {
        Specification<Show> spec = Specification.unrestricted();

        Integer mes = filtro.getMes();
        Integer ano = filtro.getAno();
        boolean mesValido = mes != null && mes >= 1 && mes <= 12;
        boolean anoValido = ano != null && ano >= 1 && ano <= 9999;

        if (anoValido) {
            LocalDate inicio = mesValido ? LocalDate.of(ano, mes, 1) : LocalDate.of(ano, Month.JANUARY, 1);
            LocalDate fim = mesValido ? inicio.plusMonths(1) : inicio.plusYears(1);
            spec = spec.and((root, query, cb) -> cb.and(
                    cb.greaterThanOrEqualTo(root.get("data"), inicio),
                    cb.lessThan(root.get("data"), fim)));
        } else if (mesValido) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(cb.function("month", Integer.class, root.get("data")), mes));
        }

        if (filtro.isSomenteFuturos()) {
            LocalDate inicioMesCorrente = LocalDate.now().withDayOfMonth(1);
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("data"), inicioMesCorrente));
        }

        if (filtro.getSituacaoPagamento() != null) {
            boolean pago = filtro.getSituacaoPagamento() == StatusPagamento.PAGO;
            spec = spec.and((root, query, cb) -> cb.equal(root.get("pago"), pago));
        }

        if (StringUtils.hasText(filtro.getArtista())) {
            spec = spec.and(contem("artista", filtro.getArtista()));
        }

        if (StringUtils.hasText(filtro.getObservacoes())) {
            spec = spec.and(contem("observacoes", filtro.getObservacoes()));
        }

        if (StringUtils.hasText(filtro.getLocal())) {
            spec = spec.and(contem("local", filtro.getLocal()));
        }

        return spec;
    }

    private static Specification<Show> contem(String atributo, String texto) {
        String padrao = "%" + escaparLike(texto.trim().toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get(atributo)), padrao, '\\');
    }

    private static String escaparLike(String texto) {
        return texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
