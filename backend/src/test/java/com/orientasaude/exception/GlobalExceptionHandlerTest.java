package com.orientasaude.exception;

import com.orientasaude.dto.response.ErroResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/triagem/99");

    @Test
    void recursoNaoEncontradoRetorna404() {
        ResponseEntity<ErroResponse> resposta =
                handler.tratarRecursoNaoEncontrado(new RecursoNaoEncontradoException("Sessão não encontrada"), request);

        assertThat(resposta.getStatusCode().value()).isEqualTo(404);
        assertThat(resposta.getBody().getMensagem()).isEqualTo("Sessão não encontrada");
        assertThat(resposta.getBody().getCaminho()).isEqualTo("/api/triagem/99");
    }

    @Test
    void erroGenericoRetorna500SemExporDetalhes() {
        ResponseEntity<ErroResponse> resposta =
                handler.tratarErroGenerico(new IllegalStateException("detalhe interno"), request);

        assertThat(resposta.getStatusCode().value()).isEqualTo(500);
        assertThat(resposta.getBody().getMensagem()).doesNotContain("detalhe interno");
    }

    @Test
    void iaIndisponivelRetorna503ComMensagemAmigavel() {
        ResponseEntity<ErroResponse> resposta =
                handler.tratarIaIndisponivel(new IaIndisponivelException("timeout interno"), request);

        assertThat(resposta.getStatusCode().value()).isEqualTo(503);
        assertThat(resposta.getBody().getMensagem()).doesNotContain("timeout interno");
    }

    @Test
    void transicaoInvalidaRetorna422() {
        ResponseEntity<ErroResponse> resposta =
                handler.tratarTransicaoInvalida(new TransicaoInvalidaException("Sessão não está em andamento"), request);

        assertThat(resposta.getStatusCode().value()).isEqualTo(422);
        assertThat(resposta.getBody().getMensagem()).isEqualTo("Sessão não está em andamento");
    }

    @Test
    void entradaInvalidaRetorna400() {
        ResponseEntity<ErroResponse> resposta =
                handler.tratarEntradaInvalida(new EntradaInvalidaException("Descreva melhor os sintomas"), request);

        assertThat(resposta.getStatusCode().value()).isEqualTo(400);
        assertThat(resposta.getBody().getMensagem()).isEqualTo("Descreva melhor os sintomas");
    }
}
