package com.example.agendashows.show.service;

import com.example.agendashows.show.dto.ShowFilter;
import com.example.agendashows.show.dto.ShowForm;
import com.example.agendashows.show.entity.Show;
import com.example.agendashows.show.exception.ShowNotFoundException;
import com.example.agendashows.show.repository.ShowRepository;
import com.example.agendashows.show.specification.ShowSpecifications;
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
