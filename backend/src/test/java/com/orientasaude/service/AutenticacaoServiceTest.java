package com.orientasaude.service;

import com.orientasaude.dto.request.LoginRequest;
import com.orientasaude.dto.request.RegistroRequest;
import com.orientasaude.exception.EmailJaCadastradoException;
import com.orientasaude.model.Usuario;
import com.orientasaude.repository.UsuarioRepository;
import com.orientasaude.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AutenticacaoServiceTest {

    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
    private final AutenticacaoService service =
            new AutenticacaoService(usuarioRepository, passwordEncoder, jwtTokenProvider);

    @Test
    void registrarComEmailExistenteLancaExcecao() {
        when(usuarioRepository.existsByEmail("maria@exemplo.com")).thenReturn(true);

        assertThatThrownBy(() -> service.registrar(
                new RegistroRequest("Maria", " Maria@Exemplo.com ", "senhaSegura123")))
                .isInstanceOf(EmailJaCadastradoException.class);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void loginDeUsuarioInativoLancaCredenciaisInvalidas() {
        Usuario inativo = Usuario.builder().email("maria@exemplo.com").senhaHash("hash").ativo(false).build();
        when(usuarioRepository.findByEmail("maria@exemplo.com")).thenReturn(Optional.of(inativo));

        assertThatThrownBy(() -> service.login(new LoginRequest("maria@exemplo.com", "senhaSegura123")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void loginComSenhaErradaLancaCredenciaisInvalidas() {
        Usuario usuario = Usuario.builder().email("maria@exemplo.com").senhaHash("hash").ativo(true).build();
        when(usuarioRepository.findByEmail("maria@exemplo.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("errada123", "hash")).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginRequest("maria@exemplo.com", "errada123")))
                .isInstanceOf(BadCredentialsException.class);
    }
}
