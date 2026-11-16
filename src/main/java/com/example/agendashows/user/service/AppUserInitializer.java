package com.example.agendashows.user.service;

import com.example.agendashows.user.entity.AppUser;
import com.example.agendashows.user.repository.AppUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class AppUserInitializer implements ApplicationRunner {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final String nomeUsuarioInicial;
    private final String senhaInicial;

    public AppUserInitializer(AppUserRepository appUserRepository,
                                PasswordEncoder passwordEncoder,
                                @Value("${app.usuario-inicial.nome:}") String nomeUsuarioInicial,
                                @Value("${app.usuario-inicial.senha:}") String senhaInicial) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.nomeUsuarioInicial = nomeUsuarioInicial;
        this.senhaInicial = senhaInicial;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (appUserRepository.count() > 0) {
            return;
        }
        if (!StringUtils.hasText(nomeUsuarioInicial) || !StringUtils.hasText(senhaInicial)) {
            log.warn("Nenhum usuário cadastrado. Defina USUARIO_INICIAL e SENHA_INICIAL para criar o usuário inicial.");
            return;
        }

        AppUser usuario = new AppUser();
        usuario.setNomeUsuario(nomeUsuarioInicial);
        usuario.setSenha(passwordEncoder.encode(senhaInicial));
        appUserRepository.save(usuario);

        log.info("Usuário inicial '{}' criado.", nomeUsuarioInicial);
    }
}
