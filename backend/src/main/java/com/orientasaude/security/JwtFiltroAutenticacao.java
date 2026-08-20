package com.orientasaude.security;

import com.orientasaude.model.Usuario;
import com.orientasaude.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * Filtro que intercepta cada requisição, extrai o token JWT do header Authorization,
 * valida e carrega o usuário no SecurityContext.
 */
@Component
@RequiredArgsConstructor
public class JwtFiltroAutenticacao extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UsuarioRepository usuarioRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = extrairToken(request);

        if (token != null && jwtTokenProvider.validarToken(token)) {
            Long usuarioId = jwtTokenProvider.extrairUsuarioId(token);

            Usuario usuario = usuarioRepository.findById(usuarioId).orElse(null);

            if (usuario != null && usuario.getAtivo()) {
                UsernamePasswordAuthenticationToken autenticacao =
                        new UsernamePasswordAuthenticationToken(usuario, null, Collections.emptyList());
                autenticacao.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(autenticacao);
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extrai o token do header "Authorization: Bearer xxx".
     */
    private String extrairToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
