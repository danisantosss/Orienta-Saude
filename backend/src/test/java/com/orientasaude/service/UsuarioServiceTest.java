package com.orientasaude.service;

import com.orientasaude.dto.response.PerfilResponse;
import com.orientasaude.exception.SenhaIncorretaException;
import com.orientasaude.model.Usuario;
import com.orientasaude.model.enums.TipoEventoAuditoria;
import com.orientasaude.repository.LogAuditoriaRepository;
import com.orientasaude.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UsuarioServiceTest {

    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final LogAuditoriaRepository logAuditoriaRepository = mock(LogAuditoriaRepository.class);
    private final AuditoriaService auditoriaService = mock(AuditoriaService.class);
    private final UsuarioService service =
            new UsuarioService(usuarioRepository, passwordEncoder, logAuditoriaRepository, auditoriaService);

    private final Usuario usuario = Usuario.builder()
            .id(7L)
            .nome("Maria Silva")
            .email("maria@exemplo.com")
            .senhaHash("hash")
            .criadoEm(LocalDateTime.of(2026, 11, 1, 10, 0))
            .consentimentoEm(LocalDateTime.of(2026, 11, 1, 10, 0))
            .versaoPolitica("1.0")
            .build();

    @Test
    void perfilTrazSoOsDadosDoProprioUsuario() {
        PerfilResponse perfil = service.obterPerfil(usuario);

        assertThat(perfil.getNome()).isEqualTo("Maria Silva");
        assertThat(perfil.getEmail()).isEqualTo("maria@exemplo.com");
        assertThat(perfil.getCriadoEm()).isEqualTo(LocalDateTime.of(2026, 11, 1, 10, 0));
        assertThat(perfil.getConsentimentoEm()).isEqualTo(LocalDateTime.of(2026, 11, 1, 10, 0));
        assertThat(perfil.getVersaoPolitica()).isEqualTo("1.0");
    }

    @Test
    void excluirContaComSenhaErradaNaoApagaNada() {
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("errada123", "hash")).thenReturn(false);

        assertThatThrownBy(() -> service.excluirConta(usuario, "errada123"))
                .isInstanceOf(SenhaIncorretaException.class);
        verify(usuarioRepository, never()).delete(any());
        verify(auditoriaService, never()).registrar(any(), any());
    }

    @Test
    void excluirContaComSenhaCertaApagaOUsuario() {
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("senhaSegura123", "hash")).thenReturn(true);

        service.excluirConta(usuario, "senhaSegura123");

        verify(usuarioRepository).delete(usuario);
        // A auditoria esquece de quem eram os eventos antigos e registra a exclusão sem id
        verify(logAuditoriaRepository).anonimizarUsuario(7L);
        verify(auditoriaService).registrar(TipoEventoAuditoria.EXCLUSAO_SOLICITADA, null);
    }
}
