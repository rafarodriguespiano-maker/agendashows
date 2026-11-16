package com.example.agendashows.user.service;

import com.example.agendashows.user.entity.AppUser;
import com.example.agendashows.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository appUserRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String nomeUsuario) throws UsernameNotFoundException {
        AppUser usuario = appUserRepository.findByNomeUsuario(nomeUsuario)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + nomeUsuario));

        return User.withUsername(usuario.getNomeUsuario())
                .password(usuario.getSenha())
                .disabled(!usuario.isAtivo())
                .authorities("ROLE_USUARIO")
                .build();
    }
}
