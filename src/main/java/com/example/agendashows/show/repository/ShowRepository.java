package com.example.agendashows.show.repository;

import com.example.agendashows.show.entity.Show;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ShowRepository extends JpaRepository<Show, Long>, JpaSpecificationExecutor<Show> {
}
