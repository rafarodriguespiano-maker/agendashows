package com.example.agendashows.dashboard.controller;

import com.example.agendashows.common.Meses;
import com.example.agendashows.dashboard.dto.PeriodoFilter;
import com.example.agendashows.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private static final String VIEW_DASHBOARD = "dashboard/index";

    private final DashboardService dashboardService;

    @GetMapping
    public String exibir(@ModelAttribute("filtro") PeriodoFilter filtro, Model model) {
        filtro.preencherPeriodoAtualSeVazio();
        model.addAttribute("meses", Meses.nomes());
        model.addAttribute("mesesGrafico", Meses.abreviados());
        model.addAttribute("resumo", dashboardService.resumir(filtro));
        model.addAttribute("ranking", dashboardService.ranking(filtro));
        model.addAttribute("proximos", dashboardService.proximosShows(filtro));
        model.addAttribute("graficos", dashboardService.graficos(filtro));
        return VIEW_DASHBOARD;
    }
}
