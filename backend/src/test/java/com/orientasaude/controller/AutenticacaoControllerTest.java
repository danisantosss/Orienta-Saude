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
                                {"nome": "Maria Silva", "email": "maria@exemplo.com", "senha": "senhaSegura123", "consentimento": true}
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
                .andExpect(jsonPath("$.campos.senha").exists())
                .andExpect(jsonPath("$.campos.consentimento").exists());
    }

    @Test
    void registrarSemAceitarAPoliticaDePrivacidadeEhRecusado() throws Exception {
        mockMvc.perform(post("/api/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome": "Ana Lima", "email": "ana@exemplo.com", "senha": "senhaSegura123", "consentimento": false}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campos.consentimento").value(
                        "Para usar a triagem é preciso aceitar a Política de Privacidade, pois ela trata dados de saúde"));
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

    @Test
    void registrarEmailDuplicadoRetornaConflito() throws Exception {
        String corpo = """
                {"nome": "João Souza", "email": "joao@exemplo.com", "senha": "senhaSegura123", "consentimento": true}
                """;
        mockMvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }
}
