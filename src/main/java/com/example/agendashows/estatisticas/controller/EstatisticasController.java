package com.example.agendashows.estatisticas.controller;

import com.example.agendashows.common.Meses;
import com.example.agendashows.estatisticas.dto.PeriodoFilter;
import com.example.agendashows.estatisticas.service.EstatisticasService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/estatisticas")
@RequiredArgsConstructor
public class EstatisticasController {

    private static final String VIEW_ESTATISTICAS = "estatisticas/index";

    private final EstatisticasService estatisticasService;

    @GetMapping
    public String exibir(@ModelAttribute("filtro") PeriodoFilter filtro, Model model) {
        filtro.preencherPeriodoAtualSeVazio();
        model.addAttribute("meses", Meses.nomes());
        model.addAttribute("resumo", estatisticasService.resumir(filtro));
        model.addAttribute("ranking", estatisticasService.ranking(filtro));
        model.addAttribute("proximos", estatisticasService.proximosShows(filtro));
        return VIEW_ESTATISTICAS;
    }
}
