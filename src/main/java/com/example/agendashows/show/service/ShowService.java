package com.example.agendashows.show.service;

import com.example.agendashows.show.dto.ShowFilter;
import com.example.agendashows.show.dto.ShowForm;
import com.example.agendashows.show.dto.ShowLoteForm;
import com.example.agendashows.show.entity.Show;
import com.example.agendashows.show.exception.ShowNotFoundException;
import com.example.agendashows.show.repository.ShowRepository;
import com.example.agendashows.show.specification.ShowSpecifications;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShowService {

    private final ShowRepository showRepository;

    @Transactional(readOnly = true)
    public Page<Show> listar(ShowFilter filtro, Pageable paginacao) {
        return showRepository.findAll(ShowSpecifications.comFiltros(filtro), paginacao);
    }

    @Transactional(readOnly = true)
    public Show buscar(Long id) {
        return showRepository.findById(id).orElseThrow(() -> new ShowNotFoundException(id));
    }

    @Transactional
    public Show criar(ShowForm formulario) {
        Show show = new Show();
        formulario.aplicarEm(show);
        return showRepository.save(show);
    }

    /**
     * Cria um show para cada ocorrência dos dias da semana selecionados no mês/ano informados,
     * copiando horário, cachê, artista, local, observações e pagamento mensal. Não verifica
     * duplicidade: datas já cadastradas recebem um novo show.
     */
    @Transactional
    public List<Show> criarEmLote(ShowLoteForm formulario) {
        List<DayOfWeek> diasSelecionados = formulario.diasSelecionados();
        YearMonth periodo = YearMonth.of(formulario.getAno(), formulario.getMes());
        List<Show> shows = new ArrayList<>();

        for (int dia = 1; dia <= periodo.lengthOfMonth(); dia++) {
            LocalDate data = periodo.atDay(dia);
            if (!diasSelecionados.contains(data.getDayOfWeek())) {
                continue;
            }
            Show show = new Show();
            show.setData(data);
            show.setArtista(formulario.getArtista());
            show.setLocal(formulario.getLocal());
            show.setHorario(formulario.getHorario());
            show.setObservacoes(formulario.getObservacoes());
            show.setCache(formulario.getCache());
            show.setPagamentoMensal(formulario.isPagamentoMensal());
            shows.add(show);
        }

        return showRepository.saveAll(shows);
    }

    @Transactional
    public Show atualizar(Long id, ShowForm formulario) {
        Show show = showRepository.findById(id).orElseThrow(() -> new ShowNotFoundException(id));
        formulario.aplicarEm(show);
        return show;
    }

    @Transactional
    public void alternarPagamento(Long id) {
        Show show = showRepository.findById(id).orElseThrow(() -> new ShowNotFoundException(id));
        show.setPago(!show.isPago());
    }

    @Transactional
    public void excluir(Long id) {
        Show show = showRepository.findById(id).orElseThrow(() -> new ShowNotFoundException(id));
        showRepository.delete(show);
    }
}
