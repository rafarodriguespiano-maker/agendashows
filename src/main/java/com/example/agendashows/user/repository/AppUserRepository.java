package com.example.agendashows.user.repository;

import com.example.agendashows.user.entity.AppUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByNomeUsuario(String nomeUsuario);
}
