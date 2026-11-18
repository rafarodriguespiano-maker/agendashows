package com.example.agendashows.common;

import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Fornece os nomes dos meses em português do Brasil para os filtros das telas.
 */
public final class Meses {

    private static final Locale LOCALE_PT_BR = Locale.of("pt", "BR");

    private Meses() {
    }

    public static Map<Integer, String> nomes() {
        Map<Integer, String> meses = new TreeMap<>();
        for (Month mes : Month.values()) {
            String nome = mes.getDisplayName(TextStyle.FULL, LOCALE_PT_BR);
            meses.put(mes.getValue(), nome.substring(0, 1).toUpperCase(LOCALE_PT_BR) + nome.substring(1));
        }
        return meses;
    }

    /**
     * Nomes abreviados dos meses em português do Brasil, na ordem de janeiro a dezembro.
     */
    public static List<String> abreviados() {
        List<String> meses = new ArrayList<>(12);
        for (Month mes : Month.values()) {
            String nome = mes.getDisplayName(TextStyle.SHORT, LOCALE_PT_BR);
            String abreviado = nome.endsWith(".") ? nome.substring(0, nome.length() - 1) : nome;
            meses.add(abreviado.substring(0, 1).toUpperCase(LOCALE_PT_BR) + abreviado.substring(1));
        }
        return meses;
    }
}
