package com.orientasaude.security;

import com.orientasaude.model.Usuario;
import com.orientasaude.model.enums.PapelUsuario;
import com.orientasaude.model.enums.TipoEventoAuditoria;
import com.orientasaude.repository.UsuarioRepository;
import com.orientasaude.service.AuditoriaService;
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
    private final AuditoriaService auditoriaService = mock(AuditoriaService.class);
    private final JwtAutenticacaoFilter filtro =
            new JwtAutenticacaoFilter(jwtTokenProvider, usuarioRepository, auditoriaService);

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
        request.addHeader("Authorization", "Bearer " + jwtTokenProvider.gerarToken(7L, "maria@exemplo.com", PapelUsuario.USUARIO));

        filtro.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal()).isEqualTo(usuario);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting("authority").containsExactly("ROLE_USUARIO");
        verify(chain).doFilter(request, response);
    }

    @Test
    void papelVemDoBancoENaoDoToken() throws Exception {
        // O token diz USUARIO, mas a conta foi promovida a ADMIN no banco depois do login
        Usuario admin = Usuario.builder().id(7L).ativo(true).papel(PapelUsuario.ADMIN).build();
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(admin));
        request.addHeader("Authorization", "Bearer " + jwtTokenProvider.gerarToken(7L, "a@exemplo.com", PapelUsuario.USUARIO));

        filtro.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting("authority").containsExactly("ROLE_ADMIN");
    }

    @Test
    void naoAutenticaUsuarioInativo() throws Exception {
        Usuario usuario = Usuario.builder().id(7L).ativo(false).build();
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));
        request.addHeader("Authorization", "Bearer " + jwtTokenProvider.gerarToken(7L, "maria@exemplo.com", PapelUsuario.USUARIO));

        filtro.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(chain).doFilter(request, response);
    }

    @Test
    void tokenInvalidoNaoAutenticaEVaiParaAAuditoria() throws Exception {
        request.addHeader("Authorization", "Bearer token-invalido");
        request.setRemoteAddr("200.1.2.3");

        filtro.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(usuarioRepository);
        verify(auditoriaService).registrar(TipoEventoAuditoria.TOKEN_INVALIDO, null, "200.1.2.3");
    }

    @Test
    void semTokenSegueSemAutenticarESemAuditoria() throws Exception {
        filtro.doFilter(new MockHttpServletRequest(), response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(usuarioRepository, auditoriaService);
    }

    @Test
    void tokenDeContaExcluidaVaiParaAAuditoria() throws Exception {
        when(usuarioRepository.findById(7L)).thenReturn(Optional.empty());
        request.addHeader("Authorization", "Bearer " + jwtTokenProvider.gerarToken(7L, "a@exemplo.com", PapelUsuario.USUARIO));

        filtro.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(auditoriaService).registrar(TipoEventoAuditoria.TOKEN_INVALIDO, null, "127.0.0.1");
    }
}
