package com.orientasaude.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Cifra e decifra textos com AES-256-GCM para guardar dados de saúde criptografados no banco.
 *
 * Formato gravado: "enc:v1:" + Base64(IV de 12 bytes + texto cifrado + tag de 16 bytes).
 * O GCM, além de esconder o conteúdo, detecta qualquer alteração (a tag não confere).
 * Um IV novo e aleatório a cada cifragem faz o mesmo texto gerar cifras diferentes.
 * Valores sem o prefixo são dados gravados antes da criptografia e são devolvidos como estão.
 */
@Component
public class Criptografador {

    private static final String PREFIXO = "enc:v1:";
    private static final String ALGORITMO = "AES/GCM/NoPadding";
    private static final int TAMANHO_IV = 12;
    private static final int TAMANHO_TAG_BITS = 128;
    private static final int TAMANHO_CHAVE = 32;

    private final SecretKey chave;
    private final SecureRandom aleatorio = new SecureRandom();

    public Criptografador(@Value("${app.criptografia.chave}") String chaveBase64) {
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(chaveBase64 == null ? "" : chaveBase64.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("A chave de criptografia precisa estar em Base64", e);
        }
        if (bytes.length != TAMANHO_CHAVE) {
            throw new IllegalStateException(
                    "A chave de criptografia precisa ter 32 bytes (256 bits). Gere com: openssl rand -base64 32");
        }
        this.chave = new SecretKeySpec(bytes, "AES");
    }

    public String cifrar(String texto) {
        if (texto == null) {
            return null;
        }
        try {
            byte[] iv = new byte[TAMANHO_IV];
            aleatorio.nextBytes(iv);
            Cipher cifra = Cipher.getInstance(ALGORITMO);
            cifra.init(Cipher.ENCRYPT_MODE, chave, new GCMParameterSpec(TAMANHO_TAG_BITS, iv));
            byte[] cifrado = cifra.doFinal(texto.getBytes(StandardCharsets.UTF_8));
            byte[] saida = ByteBuffer.allocate(iv.length + cifrado.length).put(iv).put(cifrado).array();
            return PREFIXO + Base64.getEncoder().encodeToString(saida);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Não foi possível cifrar o dado", e);
        }
    }

    public String decifrar(String valor) {
        if (valor == null || !valor.startsWith(PREFIXO)) {
            return valor;
        }
        try {
            byte[] entrada = Base64.getDecoder().decode(valor.substring(PREFIXO.length()));
            Cipher cifra = Cipher.getInstance(ALGORITMO);
            cifra.init(Cipher.DECRYPT_MODE, chave, new GCMParameterSpec(TAMANHO_TAG_BITS, entrada, 0, TAMANHO_IV));
            byte[] texto = cifra.doFinal(entrada, TAMANHO_IV, entrada.length - TAMANHO_IV);
            return new String(texto, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("Não foi possível decifrar o dado: chave errada ou conteúdo alterado", e);
        }
    }
}
