package com.orientasaude.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Conversor JPA que cifra o campo ao gravar e decifra ao ler.
 * O resto do código (services, DTOs, testes) continua trabalhando com o texto normal;
 * só o que vai para o banco fica cifrado.
 */
@Component
@Converter
@RequiredArgsConstructor
public class CriptografiaConverter implements AttributeConverter<String, String> {

    private final Criptografador criptografador;

    @Override
    public String convertToDatabaseColumn(String atributo) {
        return criptografador.cifrar(atributo);
    }

    @Override
    public String convertToEntityAttribute(String coluna) {
        return criptografador.decifrar(coluna);
    }
}
