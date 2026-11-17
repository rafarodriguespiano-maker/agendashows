package com.example.agendashows.user.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.example.agendashows.user.entity.AppUser;
import com.example.agendashows.user.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthenticationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void criarUsuario() {
        appUserRepository.deleteAll();
        AppUser usuario = new AppUser();
        usuario.setNomeUsuario("musico");
        usuario.setSenha(passwordEncoder.encode("segredo123"));
        appUserRepository.save(usuario);
    }

    @Test
    void paginaDeLoginEhPublica() throws Exception {
        mvc.perform(get("/entrar"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"));
    }

    @Test
    void loginComCredenciaisValidasRedirecionaParaEstatisticas() throws Exception {
        mvc.perform(post("/entrar").with(csrf())
                        .param("username", "musico")
                        .param("password", "segredo123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/estatisticas"));
    }

    @Test
    void raizRedirecionaParaEstatisticas() throws Exception {
        mvc.perform(get("/").with(user("musico")))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/estatisticas"));
    }

    @Test
    void loginComSenhaInvalidaRedirecionaComErro() throws Exception {
        mvc.perform(post("/entrar").with(csrf())
                        .param("username", "musico")
                        .param("password", "errada"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/entrar?erro"));
    }
}
