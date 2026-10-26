package com.orientasaude.controller;

import com.orientasaude.dto.request.EnviarMensagemRequest;
import com.orientasaude.dto.request.IniciarTriagemRequest;
import com.orientasaude.dto.response.IniciarTriagemResponse;
import com.orientasaude.dto.response.MensagemTriagemResponse;
import com.orientasaude.dto.response.PaginaResponse;
import com.orientasaude.dto.response.ResultadoTriagemResponse;
import com.orientasaude.dto.response.SessaoTriagemResponse;
import com.orientasaude.model.Usuario;
import com.orientasaude.model.enums.NivelUrgencia;
import com.orientasaude.service.HistoricoService;
import com.orientasaude.service.TriagemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Endpoints da entrevista de triagem (exigem token JWT).
 * O usuário vem do token, nunca do corpo da requisição.
 */
@RestController
@RequestMapping("/api/triagem")
@RequiredArgsConstructor
public class TriagemController {

    private final TriagemService triagemService;
    private final HistoricoService historicoService;

    /**
     * POST /api/triagem/iniciar
     * Cria a sessão e devolve a primeira pergunta.
     */
    @PostMapping("/iniciar")
    public ResponseEntity<IniciarTriagemResponse> iniciar(
            @Valid @RequestBody IniciarTriagemRequest request,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.status(HttpStatus.CREATED).body(triagemService.iniciar(request, usuario));
    }

    /**
     * POST /api/triagem/{id}/mensagem
     * Envia a resposta do usuário e devolve a próxima pergunta (ou o fim da entrevista).
     */
    @PostMapping("/{id}/mensagem")
    public ResponseEntity<MensagemTriagemResponse> enviarMensagem(
            @PathVariable Long id,
            @Valid @RequestBody EnviarMensagemRequest request,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(triagemService.enviarMensagem(id, request, usuario));
    }

    /**
     * POST /api/triagem/{id}/encerrar
     * Encerra a entrevista antes da conclusão.
     */
    @PostMapping("/{id}/encerrar")
    public ResponseEntity<MensagemTriagemResponse> encerrar(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(triagemService.encerrar(id, usuario));
    }

    /**
     * POST /api/triagem/{id}/analisar
     * Gera o resultado com RAG + Gemini. Repetir a chamada devolve o mesmo resultado.
     */
    @PostMapping("/{id}/analisar")
    public ResponseEntity<ResultadoTriagemResponse> analisar(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(triagemService.analisar(id, usuario));
    }

    /**
     * GET /api/triagem/{id}/resultado
     * Devolve o resultado de uma triagem concluída.
     */
    @GetMapping("/{id}/resultado")
    public ResponseEntity<ResultadoTriagemResponse> obterResultado(
            @PathVariable Long id,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(triagemService.obterResultado(id, usuario));
    }

    /**
     * GET /api/triagem/historico?pagina=0&tamanho=20&inicio=2026-04-01&fim=2026-04-30&nivelUrgencia=URGENTE
     * Triagens do usuário, da mais recente para a mais antiga. Todos os filtros são opcionais.
     */
    @GetMapping("/historico")
    public ResponseEntity<PaginaResponse<SessaoTriagemResponse>> listarHistorico(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamanho,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) NivelUrgencia nivelUrgencia,
            @AuthenticationPrincipal Usuario usuario) {
        return ResponseEntity.ok(historicoService.listar(usuario, pagina, tamanho, inicio, fim, nivelUrgencia));
    }
}
