package com.example.agendashows.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String ENTRAR = "/entrar";

    @Bean
    public SecurityFilterChain securityFilter(HttpSecurity http) {
        http.authorizeHttpRequests(autorizacao -> autorizacao
                        .requestMatchers(ENTRAR, "/error", "/css/**", "/js/**", "/img/**", "/calendario/**").permitAll()
                        .anyRequest().authenticated())
                .formLogin(login -> login
                        .loginPage(ENTRAR)
                        .loginProcessingUrl(ENTRAR)
                        .defaultSuccessUrl("/estatisticas", true)
                        .failureUrl(ENTRAR + "?erro")
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/sair")
                        .logoutSuccessUrl(ENTRAR + "?saiu")
                        .permitAll());
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
