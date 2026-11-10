package com.orientasaude.config;

import com.orientasaude.model.Usuario;
import com.orientasaude.model.enums.TipoEventoAuditoria;
import com.orientasaude.security.JwtAutenticacaoFilter;
import com.orientasaude.security.RespostaErroJson;
import com.orientasaude.service.AuditoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuração de segurança do Spring Security.
 * - Cadastro, login e health check são públicos
 * - /api/admin/** exige o papel ADMIN (sem ele: 403)
 * - Todo o resto exige token JWT válido (sem ele: 401)
 * - Sessão stateless (sem cookies de sessão), por isso o CSRF fica desligado
 * - CORS conforme CorsConfig
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SegurancaConfig {

    private final JwtAutenticacaoFilter jwtAutenticacaoFilter;
    private final RespostaErroJson respostaErroJson;
    private final AuditoriaService auditoriaService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .exceptionHandling(erros -> erros
                .authenticationEntryPoint((request, response, excecao) ->
                    respostaErroJson.escrever(request, response, HttpStatus.UNAUTHORIZED,
                            "Não Autorizado", "Faça login para continuar."))
                .accessDeniedHandler((request, response, excecao) -> {
                    auditoriaService.registrar(TipoEventoAuditoria.PERMISSAO_NEGADA, usuarioLogadoId());
                    respostaErroJson.escrever(request, response, HttpStatus.FORBIDDEN,
                            "Proibido", "Você não tem permissão para acessar este recurso.");
                })
            )
            .addFilterBefore(jwtAutenticacaoFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private static Long usuarioLogadoId() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao != null && autenticacao.getPrincipal() instanceof Usuario usuario) {
            return usuario.getId();
        }
        return null;
    }
}
