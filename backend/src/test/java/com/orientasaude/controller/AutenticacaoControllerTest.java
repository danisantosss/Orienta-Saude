package com.orientasaude.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AutenticacaoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void registrarELogarRetornaToken() throws Exception {
        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Maria Silva", "email": "maria@exemplo.com", "senha": "senhaSegura123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mensagem").value("Cadastro realizado com sucesso"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "maria@exemplo.com", "senha": "senhaSegura123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.expiraEm").value(86400000));
    }

    @Test
    void registrarComCamposInvalidosRetornaErrosPorCampo() throws Exception {
        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "M", "email": "invalido", "senha": "123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.caminho").value("/api/auth/registro"))
                .andExpect(jsonPath("$.campos.nome").exists())
                .andExpect(jsonPath("$.campos.email").exists())
                .andExpect(jsonPath("$.campos.senha").exists());
    }

    @Test
    void loginComCredenciaisInvalidasRetornaNaoAutorizado() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "ninguem@exemplo.com", "senha": "qualquer123"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("Credenciais inválidas"));
    }
}
