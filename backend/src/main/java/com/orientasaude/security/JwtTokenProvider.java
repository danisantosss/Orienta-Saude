package com.orientasaude.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Componente responsável por gerar e validar tokens JWT.
 * O token contém o ID do usuário como subject e expira em 24 horas.
 */
@Component
public class JwtTokenProvider {

    private final SecretKey chaveSecreta;
    private final long expiracaoMs;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String segredo,
            @Value("${app.jwt.expiration-ms}") long expiracaoMs) {
        this.chaveSecreta = Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8));
        this.expiracaoMs = expiracaoMs;
    }

    /**
     * Gera um token JWT para o usuário informado.
     */
    public String gerarToken(Long usuarioId, String email) {
        Date agora = new Date();
        Date expiracao = new Date(agora.getTime() + expiracaoMs);

        return Jwts.builder()
                .subject(usuarioId.toString())
                .claim("email", email)
                .issuedAt(agora)
                .expiration(expiracao)
                .signWith(chaveSecreta)
                .compact();
    }

    /**
     * Extrai o ID do usuário a partir do token.
     */
    public Long extrairUsuarioId(String token) {
        Claims claims = extrairClaims(token);
        return Long.parseLong(claims.getSubject());
    }

    /**
     * Valida se o token é íntegro e não expirou.
     */
    public boolean validarToken(String token) {
        try {
            extrairClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Retorna o tempo de expiração em milissegundos.
     */
    public long getExpiracaoMs() {
        return expiracaoMs;
    }

    private Claims extrairClaims(String token) {
        return Jwts.parser()
                .verifyWith(chaveSecreta)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
