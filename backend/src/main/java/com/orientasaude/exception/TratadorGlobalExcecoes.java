package com.orientasaude.exception;

import com.orientasaude.dto.response.ErroResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Handler global que captura exceções e retorna respostas padronizadas.
 * Não expõe stack traces ao cliente.
 */
@RestControllerAdvice
public class TratadorGlobalExcecoes {

    private static final Logger log = LoggerFactory.getLogger(TratadorGlobalExcecoes.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> tratarValidacao(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        Map<String, String> campos = new HashMap<>();
        for (FieldError erro : ex.getBindingResult().getFieldErrors()) {
            campos.put(erro.getField(), erro.getDefaultMessage());
        }

        ErroResponse resposta = ErroResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .erro("Erro de Validação")
                .mensagem("Um ou mais campos não atendem aos critérios de validação")
                .caminho(request.getRequestURI())
                .campos(campos)
                .build();

        return ResponseEntity.badRequest().body(resposta);
    }

    @ExceptionHandler(EmailJaCadastradoException.class)
    public ResponseEntity<ErroResponse> tratarEmailDuplicado(
            EmailJaCadastradoException ex, HttpServletRequest request) {

        ErroResponse resposta = ErroResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .erro("Conflito")
                .mensagem(ex.getMessage())
                .caminho(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(resposta);
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> tratarRecursoNaoEncontrado(
            RecursoNaoEncontradoException ex, HttpServletRequest request) {

        ErroResponse resposta = ErroResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .erro("Não Encontrado")
                .mensagem(ex.getMessage())
                .caminho(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(resposta);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> tratarErroGenerico(
            Exception ex, HttpServletRequest request) {

        log.error("Erro inesperado em {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        ErroResponse resposta = ErroResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .erro("Erro Interno")
                .mensagem("Ocorreu um erro inesperado. Tente novamente mais tarde.")
                .caminho(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(resposta);
    }
}
