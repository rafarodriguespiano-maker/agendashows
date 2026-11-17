package com.example.agendashows.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.agendashows.user.entity.AppUser;
import com.example.agendashows.user.repository.AppUserRepository;
import java.util.Optional;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AppUserInitializerTest {

    @Mock
    private AppUserRepository appUserRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void naoCriaUsuarioQuandoJaExisteAlgumCadastrado() {
        when(appUserRepository.count()).thenReturn(1L);
        AppUserInitializer inicializador = new AppUserInitializer(
                appUserRepository, passwordEncoder, "musico", "segredo123", "");

        inicializador.run(null);

        verify(appUserRepository, never()).save(any(AppUser.class));
    }

    @Test
    void criaUsuarioInicialComSenhaCodificada() {
        when(appUserRepository.count()).thenReturn(0L);
        AppUserInitializer inicializador = new AppUserInitializer(
                appUserRepository, passwordEncoder, "musico", "segredo123", "musico@exemplo.com");

        inicializador.run(null);

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(captor.capture());
        AppUser salvo = captor.getValue();
        assertThat(salvo.getNomeUsuario()).isEqualTo("musico");
        assertThat(salvo.getSenha()).isNotEqualTo("segredo123");
        assertThat(passwordEncoder.matches("segredo123", salvo.getSenha())).isTrue();
        assertThat(salvo.getEmail()).isEqualTo("musico@exemplo.com");
    }

    @Test
    void naoCriaUsuarioSemCredenciaisConfiguradas() {
        when(appUserRepository.count()).thenReturn(0L);
        AppUserInitializer inicializador = new AppUserInitializer(
                appUserRepository, passwordEncoder, "", "", "");

        inicializador.run(null);

        verify(appUserRepository, never()).save(any(AppUser.class));
    }

    @Test
    void preencheEmailDoUsuarioExistenteSemEmail() {
        when(appUserRepository.count()).thenReturn(1L);
        AppUser existente = new AppUser();
        existente.setNomeUsuario("musico");
        when(appUserRepository.findFirstByAtivoTrueOrderByIdAsc()).thenReturn(Optional.of(existente));

        AppUserInitializer inicializador = new AppUserInitializer(
                appUserRepository, passwordEncoder, "admin", "admin", "principal@exemplo.com");

        inicializador.run(null);

        verify(appUserRepository).save(existente);
        assertThat(existente.getEmail()).isEqualTo("principal@exemplo.com");
    }

    @Test
    void naoSobrescreveEmailJaCadastrado() {
        when(appUserRepository.count()).thenReturn(1L);
        AppUser existente = new AppUser();
        existente.setNomeUsuario("musico");
        existente.setEmail("antigo@exemplo.com");
        when(appUserRepository.findFirstByAtivoTrueOrderByIdAsc()).thenReturn(Optional.of(existente));

        AppUserInitializer inicializador = new AppUserInitializer(
                appUserRepository, passwordEncoder, "admin", "admin", "novo@exemplo.com");

        inicializador.run(null);

        verify(appUserRepository, never()).save(any(AppUser.class));
        assertThat(existente.getEmail()).isEqualTo("antigo@exemplo.com");
    }
}
