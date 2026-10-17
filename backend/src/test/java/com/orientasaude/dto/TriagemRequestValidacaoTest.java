package com.orientasaude.dto;

import com.orientasaude.dto.request.EnviarMensagemRequest;
import com.orientasaude.dto.request.IniciarTriagemRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Task 9.2: limites de validação da entrada da triagem (Requisitos 3.2, 3.3 e 3.4).
 */
class TriagemRequestValidacaoTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private Set<String> camposInvalidos(Object request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(java.util.stream.Collectors.toSet());
    }

    @ParameterizedTest
    @ValueSource(ints = {10, 500, 1000})
    void aceitaSintomasEntre10E1000Caracteres(int tamanho) {
        assertThat(camposInvalidos(new IniciarTriagemRequest("a".repeat(tamanho), 30))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 9, 1001})
    void rejeitaSintomasForaDosLimites(int tamanho) {
        assertThat(camposInvalidos(new IniciarTriagemRequest("a".repeat(tamanho), 30))).contains("sintomas");
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 60, 120})
    void aceitaIdadeEntre1E120(int idade) {
        assertThat(camposInvalidos(new IniciarTriagemRequest("Dor de cabeça forte", idade))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 121})
    void rejeitaIdadeForaDosLimites(int idade) {
        assertThat(camposInvalidos(new IniciarTriagemRequest("Dor de cabeça forte", idade))).contains("idade");
    }

    @Test
    void rejeitaIdadeAusente() {
        assertThat(camposInvalidos(new IniciarTriagemRequest("Dor de cabeça forte", null))).contains("idade");
    }

    @Test
    void rejeitaRespostaVaziaOuMaiorQue1000Caracteres() {
        assertThat(camposInvalidos(new EnviarMensagemRequest("   "))).contains("conteudo");
        assertThat(camposInvalidos(new EnviarMensagemRequest("a".repeat(1001)))).contains("conteudo");
        assertThat(camposInvalidos(new EnviarMensagemRequest("Sim, há dois dias"))).isEmpty();
    }
}
