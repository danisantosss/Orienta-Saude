package com.orientasaude.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orientasaude.dto.response.ErroResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * Escreve um ErroResponse em JSON direto na resposta. Usado pelos filtros e pelos tratadores do
 * Spring Security, que agem antes dos controllers e por isso não passam pelo GlobalExceptionHandler.
 * Assim o frontend recebe o mesmo formato de erro em qualquer caso.
 */
@Component
@RequiredArgsConstructor
public class RespostaErroJson {

    private final ObjectMapper objectMapper;

    public void escrever(HttpServletRequest request, HttpServletResponse response,
                         HttpStatus status, String erro, String mensagem) throws IOException {
        ErroResponse corpo = ErroResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .erro(erro)
                .mensagem(mensagem)
                .caminho(request.getRequestURI())
                .build();
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), corpo);
    }
}
