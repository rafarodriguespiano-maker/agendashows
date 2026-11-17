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
    private final String emailInicial;

    public AppUserInitializer(AppUserRepository appUserRepository,
                                PasswordEncoder passwordEncoder,
                                @Value("${app.usuario-inicial.nome:}") String nomeUsuarioInicial,
                                @Value("${app.usuario-inicial.senha:}") String senhaInicial,
                                @Value("${app.usuario-inicial.email:}") String emailInicial) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.nomeUsuarioInicial = nomeUsuarioInicial;
        this.senhaInicial = senhaInicial;
        this.emailInicial = emailInicial;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (appUserRepository.count() > 0) {
            preencherEmailUsuarioPrincipal();
            return;
        }
        if (!StringUtils.hasText(nomeUsuarioInicial) || !StringUtils.hasText(senhaInicial)) {
            log.warn("Nenhum usuário cadastrado. Defina USUARIO_INICIAL e SENHA_INICIAL para criar o usuário inicial.");
            return;
        }

        AppUser usuario = new AppUser();
        usuario.setNomeUsuario(nomeUsuarioInicial);
        usuario.setSenha(passwordEncoder.encode(senhaInicial));
        if (StringUtils.hasText(emailInicial)) {
            usuario.setEmail(emailInicial.trim());
        }
        appUserRepository.save(usuario);

        log.info("Usuário inicial '{}' criado.", nomeUsuarioInicial);
    }

    /**
     * Mantém o destinatário do relatório semanal configurado: se já existe usuário e ele ainda não
     * possui e-mail, aproveita o {@code EMAIL_INICIAL} informado no start da aplicação.
     */
    private void preencherEmailUsuarioPrincipal() {
        if (!StringUtils.hasText(emailInicial)) {
            return;
        }
        appUserRepository.findFirstByAtivoTrueOrderByIdAsc()
                .filter(usuario -> !StringUtils.hasText(usuario.getEmail()))
                .ifPresent(usuario -> {
                    usuario.setEmail(emailInicial.trim());
                    appUserRepository.save(usuario);
                    log.info("E-mail do usuário '{}' preenchido a partir de EMAIL_INICIAL.", usuario.getNomeUsuario());
                });
    }
}
