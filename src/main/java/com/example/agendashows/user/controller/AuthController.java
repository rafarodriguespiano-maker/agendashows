package com.example.agendashows.user.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {

    @GetMapping("/")
    public String raiz() {
        return "redirect:/shows";
    }

    @GetMapping("/entrar")
    public String entrar() {
        return "auth/login";
    }
}
