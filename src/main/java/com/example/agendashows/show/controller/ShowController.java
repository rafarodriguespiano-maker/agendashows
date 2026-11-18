package com.example.agendashows.show.controller;

import com.example.agendashows.common.Meses;
import com.example.agendashows.show.dto.ShowFilter;
import com.example.agendashows.show.dto.ShowForm;
import com.example.agendashows.show.dto.ShowLoteForm;
import com.example.agendashows.show.entity.Show;
import com.example.agendashows.show.entity.enums.StatusPagamento;
import com.example.agendashows.show.exception.ShowNotFoundException;
import com.example.agendashows.show.service.ShowService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.Formatter;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Controller
@RequestMapping("/shows")
@RequiredArgsConstructor
public class ShowController {

    private static final String VIEW_FORMULARIO = "shows/form";
    private static final String VIEW_LOTE = "shows/lote";
    private static final String FLASH_MENSAGEM = "mensagem";
    private static final String FLASH_ERRO = "erro";
    private static final String REDIRECT_SHOWS = "redirect:/shows";

    private final ShowService showService;

    @Value("${app.calendario.token:}")
    private String tokenCalendario;

    @GetMapping
    public String listar(@ModelAttribute("filtro") ShowFilter filtro,
                         @PageableDefault(size = 20, sort = "data", direction = Sort.Direction.ASC) Pageable paginacao,
                         HttpServletRequest requisicao,
                         Model model) {
        model.addAttribute("pagina", showService.listar(filtro, paginacao));
        model.addAttribute("meses", Meses.nomes());
        model.addAttribute("situacoes", StatusPagamento.values());
        model.addAttribute("parametrosLista", requisicao.getQueryString());
        if (StringUtils.hasText(tokenCalendario)) {
            model.addAttribute("calendarioToken", tokenCalendario);
        }
        adicionarFiltroAoModelo(filtro.getMes(), filtro.getAno(), model);
        return "shows/list";
    }

    // Converte LocalDate em ISO (yyyy-MM-dd) apenas neste controller, formato exigido por <input type="date">.
    @InitBinder
    void configurarData(WebDataBinder binder) {
        binder.addCustomFormatter(new Formatter<LocalDate>() {
            @Override
            public LocalDate parse(String texto, Locale locale) {
                return StringUtils.hasText(texto) ? LocalDate.parse(texto) : null;
            }

            @Override
            public String print(LocalDate data, Locale locale) {
                return data == null ? "" : data.toString();
            }
        });
    }

    @GetMapping("/novo")
    public String novo(@RequestParam(required = false) Integer mes,
                       @RequestParam(required = false) Integer ano,
                       Model model) {
        model.addAttribute("formulario", new ShowForm());
        adicionarFiltroAoModelo(mes, ano, model);
        return VIEW_FORMULARIO;
    }

    @GetMapping("/lote")
    public String novoEmLote(@RequestParam(required = false) Integer mes,
                             @RequestParam(required = false) Integer ano,
                             Model model) {
        ShowLoteForm formulario = new ShowLoteForm();
        YearMonth atual = YearMonth.now();
        formulario.setMes(mes != null && mes >= 1 && mes <= 12 ? mes : atual.getMonthValue());
        formulario.setAno(ano != null && ano >= 1 && ano <= 9999 ? ano : atual.getYear());
        model.addAttribute("formulario", formulario);
        model.addAttribute("meses", Meses.nomes());
        adicionarFiltroAoModelo(formulario.getMes(), formulario.getAno(), model);
        return VIEW_LOTE;
    }

    @PostMapping("/lote")
    public String criarEmLote(@Valid @ModelAttribute("formulario") ShowLoteForm formulario,
                              BindingResult resultado,
                              RedirectAttributes redirecionamento,
                              Model model) {
        if (resultado.hasErrors()) {
            model.addAttribute("meses", Meses.nomes());
            adicionarFiltroAoModelo(formulario.getMes(), formulario.getAno(), model);
            return VIEW_LOTE;
        }
        List<Show> criados = showService.criarEmLote(formulario);
        String mensagem = criados.size() == 1
                ? "1 show cadastrado com sucesso."
                : criados.size() + " shows cadastrados com sucesso.";
        redirecionamento.addFlashAttribute(FLASH_MENSAGEM, mensagem);
        return redirecionarParaLista(formulario.getMes(), formulario.getAno());
    }

    @PostMapping
    public String criar(@Valid @ModelAttribute("formulario") ShowForm formulario,
                        BindingResult resultado,
                        @RequestParam(required = false) Integer mes,
                        @RequestParam(required = false) Integer ano,
                        RedirectAttributes redirecionamento,
                        Model model) {
        if (resultado.hasErrors()) {
            adicionarFiltroAoModelo(mes, ano, model);
            return VIEW_FORMULARIO;
        }
        showService.criar(formulario);
        redirecionamento.addFlashAttribute(FLASH_MENSAGEM, "Show cadastrado com sucesso.");
        return redirecionarParaLista(mes, ano);
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id,
                         @RequestParam(required = false) Integer mes,
                         @RequestParam(required = false) Integer ano,
                         Model model) {
        model.addAttribute("formulario", ShowForm.de(showService.buscar(id)));
        adicionarFiltroAoModelo(mes, ano, model);
        return VIEW_FORMULARIO;
    }

    @PostMapping("/{id}")
    public String atualizar(@PathVariable Long id,
                            @Valid @ModelAttribute("formulario") ShowForm formulario,
                            BindingResult resultado,
                            @RequestParam(required = false) Integer mes,
                            @RequestParam(required = false) Integer ano,
                            RedirectAttributes redirecionamento,
                            Model model) {
        if (resultado.hasErrors()) {
            formulario.setId(id);
            adicionarFiltroAoModelo(mes, ano, model);
            return VIEW_FORMULARIO;
        }
        showService.atualizar(id, formulario);
        redirecionamento.addFlashAttribute(FLASH_MENSAGEM, "Show atualizado com sucesso.");
        return redirecionarParaLista(mes, ano);
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

    private String redirecionarParaLista(Integer mes, Integer ano) {
        List<String> parametros = new ArrayList<>();
        if (mes != null && mes >= 1 && mes <= 12) {
            parametros.add("mes=" + mes);
        }
        if (ano != null && ano >= 1 && ano <= 9999) {
            parametros.add("ano=" + ano);
        }
        return parametros.isEmpty() ? REDIRECT_SHOWS : REDIRECT_SHOWS + "?" + String.join("&", parametros);
    }

    private void adicionarFiltroAoModelo(Integer mes, Integer ano, Model model) {
        if (mes != null && mes >= 1 && mes <= 12) {
            model.addAttribute("mes", mes);
        }
        if (ano != null && ano >= 1 && ano <= 9999) {
            model.addAttribute("ano", ano);
        }
    }

}
