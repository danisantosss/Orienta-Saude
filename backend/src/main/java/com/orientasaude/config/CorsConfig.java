package com.orientasaude.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * CORS: quais sites, além do próprio, podem chamar a API pelo navegador.
 *
 * Em homologação e produção o navegador fala só com o nginx do frontend, que repassa /api para o
 * backend (mesma origem), então nenhuma origem externa precisa ser liberada e a lista fica vazia.
 * Em desenvolvimento a lista libera o Vite (localhost:3000 e 5173).
 * A lista vem de app.cors.origens-permitidas (variável APP_CORS_ORIGENS, separada por vírgula).
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.origens-permitidas:}") String origensPermitidas) {
        List<String> origens = Arrays.stream(origensPermitidas.split(","))
                .map(String::trim)
                .filter(origem -> !origem.isEmpty())
                .toList();

        CorsConfiguration configuracao = new CorsConfiguration();
        configuracao.setAllowedOrigins(origens);
        configuracao.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuracao.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuracao.setExposedHeaders(List.of("Retry-After"));
        // O token vai no cabeçalho Authorization, não em cookie: não há credenciais de navegador
        configuracao.setAllowCredentials(false);
        configuracao.setMaxAge(Duration.ofHours(1));

        UrlBasedCorsConfigurationSource fonte = new UrlBasedCorsConfigurationSource();
        fonte.registerCorsConfiguration("/api/**", configuracao);
        return fonte;
    }
}
