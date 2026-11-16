package com.example.agendashows.show.controller;

import com.example.agendashows.show.dto.ShowFilter;
import com.example.agendashows.show.dto.ShowForm;
import com.example.agendashows.show.entity.enums.StatusPagamento;
import com.example.agendashows.show.exception.ShowNotFoundException;
import com.example.agendashows.show.service.ShowService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

@Controller
@RequestMapping("/shows")
@RequiredArgsConstructor
public class ShowController {

    private static final Locale LOCALE_PT_BR = Locale.of("pt", "BR");
    private static final String VIEW_FORMULARIO = "shows/form";
    private static final String FLASH_MENSAGEM = "mensagem";
    private static final String FLASH_ERRO = "erro";
    private static final String REDIRECT_SHOWS = "redirect:/shows";

    private final ShowService showService;

    @GetMapping
    public String listar(@ModelAttribute("filtro") ShowFilter filtro,
                         @PageableDefault(size = 20, sort = "data", direction = Sort.Direction.ASC) Pageable paginacao,
                         HttpServletRequest requisicao,
                         Model model) {
        model.addAttribute("pagina", showService.listar(filtro, paginacao));
        model.addAttribute("meses", nomesDosMeses());
        model.addAttribute("situacoes", StatusPagamento.values());
        model.addAttribute("parametrosLista", requisicao.getQueryString());
        return "shows/list";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("formulario", new ShowForm());
        return VIEW_FORMULARIO;
    }

    @PostMapping
    public String criar(@Valid @ModelAttribute("formulario") ShowForm formulario,
                        BindingResult resultado,
                        RedirectAttributes redirecionamento) {
        if (resultado.hasErrors()) {
            return VIEW_FORMULARIO;
        }
        showService.criar(formulario);
        redirecionamento.addFlashAttribute(FLASH_MENSAGEM, "Show cadastrado com sucesso.");
        return REDIRECT_SHOWS;
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("formulario", ShowForm.de(showService.buscar(id)));
        return VIEW_FORMULARIO;
    }

    @PostMapping("/{id}")
    public String atualizar(@PathVariable Long id,
                            @Valid @ModelAttribute("formulario") ShowForm formulario,
                            BindingResult resultado,
                            RedirectAttributes redirecionamento) {
        if (resultado.hasErrors()) {
            formulario.setId(id);
            return VIEW_FORMULARIO;
        }
        showService.atualizar(id, formulario);
        redirecionamento.addFlashAttribute(FLASH_MENSAGEM, "Show atualizado com sucesso.");
        return REDIRECT_SHOWS;
    }

    @PostMapping("/{id}/pagamento")
    public String alternarPagamento(@PathVariable Long id,
                                    @RequestParam(required = false) String filtros,
                                    RedirectAttributes redirecionamento) {
        showService.alternarPagamento(id);
        redirecionamento.addFlashAttribute(FLASH_MENSAGEM, "Situação de pagamento atualizada.");
        return redirecionarParaLista(filtros);
    }

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Long id,
                          @RequestParam(required = false) String filtros,
                          RedirectAttributes redirecionamento) {
        showService.excluir(id);
        redirecionamento.addFlashAttribute(FLASH_MENSAGEM, "Show excluído com sucesso.");
        return redirecionarParaLista(filtros);
    }

    @ExceptionHandler(ShowNotFoundException.class)
    public String naoEncontrado(RedirectAttributes redirecionamento) {
        redirecionamento.addFlashAttribute(FLASH_ERRO, "Show não encontrado.");
        return REDIRECT_SHOWS;
    }

    private String redirecionarParaLista(String filtros) {
        if (filtros == null || !filtros.matches("[\\w=&.%+-]+")) {
            return REDIRECT_SHOWS;
        }
        return REDIRECT_SHOWS + "?" + filtros;
    }

    private Map<Integer, String> nomesDosMeses() {
        Map<Integer, String> meses = new TreeMap<>();
        for (Month mes : Month.values()) {
            String nome = mes.getDisplayName(TextStyle.FULL, LOCALE_PT_BR);
            meses.put(mes.getValue(), nome.substring(0, 1).toUpperCase(LOCALE_PT_BR) + nome.substring(1));
        }
        return meses;
    }
}
