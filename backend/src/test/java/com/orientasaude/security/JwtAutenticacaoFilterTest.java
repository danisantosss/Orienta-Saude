package com.orientasaude.security;

import com.orientasaude.model.Usuario;
import com.orientasaude.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class JwtAutenticacaoFilterTest {

    private final JwtTokenProvider jwtTokenProvider =
            new JwtTokenProvider("segredo-de-teste-com-pelo-menos-256-bits-para-hs256", 60_000);
    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final JwtAutenticacaoFilter filtro = new JwtAutenticacaoFilter(jwtTokenProvider, usuarioRepository);

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain chain;

    @BeforeEach
    void preparar() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        chain = mock(FilterChain.class);
    }

    @AfterEach
    void limpar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void autenticaUsuarioAtivoComTokenValido() throws Exception {
        Usuario usuario = Usuario.builder().id(7L).email("maria@exemplo.com").ativo(true).build();
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));
        request.addHeader("Authorization", "Bearer " + jwtTokenProvider.gerarToken(7L, "maria@exemplo.com"));

        filtro.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(usuario);
        verify(chain).doFilter(request, response);
    }

    @Test
    void naoAutenticaUsuarioInativo() throws Exception {
        Usuario usuario = Usuario.builder().id(7L).ativo(false).build();
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));
        request.addHeader("Authorization", "Bearer " + jwtTokenProvider.gerarToken(7L, "maria@exemplo.com"));

        filtro.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    void ignoraTokenInvalidoOuAusente() throws Exception {
        request.addHeader("Authorization", "Bearer token-invalido");

        filtro.doFilter(request, response, chain);
        filtro.doFilter(new MockHttpServletRequest(), response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(usuarioRepository);
    }
}
