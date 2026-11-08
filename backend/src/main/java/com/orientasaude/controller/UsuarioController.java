package com.orientasaude.controller;

import com.orientasaude.dto.request.ExcluirContaRequest;
import com.orientasaude.dto.response.MensagemResponse;
import com.orientasaude.dto.response.PerfilResponse;
import com.orientasaude.model.Usuario;
import com.orientasaude.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints da conta do usuário logado (exigem token JWT).
 */
@RestController
@RequestMapping("/api/usuario")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    /**
     * GET /api/usuario/perfil
     * Nome, email, data de cadastro e data do consentimento.
     */
    @GetMapping("/perfil")
    public ResponseEntity<PerfilResponse> obterPerfil(@AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(usuarioService.obterPerfil(usuario));
    }

    /**
     * DELETE /api/usuario/conta
     * Exclui a conta e todas as triagens. Pede a senha no corpo para confirmar.
     */
    @DeleteMapping("/conta")
    public ResponseEntity<MensagemResponse> excluirConta(
            @Valid @RequestBody ExcluirContaRequest request,
            @AuthenticationPrincipal Usuario usuario) {
        usuarioService.excluirConta(usuario, request.getSenha());
        return ResponseEntity.ok(new MensagemResponse(
                "Recebemos sua solicitação. Sua conta e todos os seus dados foram excluídos."));
    }
}
