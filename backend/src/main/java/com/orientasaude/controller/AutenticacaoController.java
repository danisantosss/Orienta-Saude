package com.orientasaude.controller;

import com.orientasaude.dto.request.LoginRequest;
import com.orientasaude.dto.request.RegistroRequest;
import com.orientasaude.dto.response.AutenticacaoResponse;
import com.orientasaude.dto.response.MensagemResponse;
import com.orientasaude.service.AutenticacaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller responsável pelos endpoints de autenticação (públicos).
 * Cadastro e login não exigem token JWT.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AutenticacaoController {

    private final AutenticacaoService autenticacaoService;

    /**
     * POST /api/auth/registro
     * Cadastra um novo usuário.
     */
    @PostMapping("/registro")
    public ResponseEntity<MensagemResponse> registrar(@Valid @RequestBody RegistroRequest request) {
        autenticacaoService.registrar(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new MensagemResponse("Cadastro realizado com sucesso"));
    }

    /**
     * POST /api/auth/login
     * Autentica um usuário e retorna o token JWT.
     */
    @PostMapping("/login")
    public ResponseEntity<AutenticacaoResponse> login(@Valid @RequestBody LoginRequest request) {
        AutenticacaoResponse resposta = autenticacaoService.login(request);
        return ResponseEntity.ok(resposta);
    }
}
