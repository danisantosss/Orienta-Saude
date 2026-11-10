package com.orientasaude.security;

import com.orientasaude.model.Usuario;
import com.orientasaude.model.enums.TipoEventoAuditoria;
import com.orientasaude.repository.UsuarioRepository;
import com.orientasaude.service.AuditoriaService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtro que intercepta cada requisição, extrai o token JWT do header Authorization,
 * valida e carrega o usuário no SecurityContext, com o papel dele (ROLE_USUARIO ou ROLE_ADMIN).
 *
 * O papel vem do banco, não do token: se alguém perde o papel ADMIN, perde o acesso na hora,
 * sem esperar o token expirar. Token adulterado, expirado ou de conta excluída fica na auditoria.
 */
@Component
@RequiredArgsConstructor
public class JwtAutenticacaoFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoriaService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = extrairToken(request);

        if (token != null) {
            Usuario usuario = jwtTokenProvider.validarToken(token)
                    ? usuarioRepository.findById(jwtTokenProvider.extrairUsuarioId(token)).orElse(null)
                    : null;

            if (usuario != null && Boolean.TRUE.equals(usuario.getAtivo())) {
                List<SimpleGrantedAuthority> papeis =
                        List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getPapel().name()));
                UsernamePasswordAuthenticationToken autenticacao =
                        new UsernamePasswordAuthenticationToken(usuario, null, papeis);
                autenticacao.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(autenticacao);
            } else {
                auditoriaService.registrar(TipoEventoAuditoria.TOKEN_INVALIDO, null, request.getRemoteAddr());
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
