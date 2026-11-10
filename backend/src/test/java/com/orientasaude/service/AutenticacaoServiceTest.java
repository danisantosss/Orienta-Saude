package com.orientasaude.service;

import com.orientasaude.dto.request.LoginRequest;
import com.orientasaude.dto.request.RegistroRequest;
import com.orientasaude.dto.response.AutenticacaoResponse;
import com.orientasaude.exception.EmailJaCadastradoException;
import com.orientasaude.exception.LoginBloqueadoException;
import com.orientasaude.model.Usuario;
import com.orientasaude.model.enums.PapelUsuario;
import com.orientasaude.model.enums.TipoEventoAuditoria;
import com.orientasaude.repository.UsuarioRepository;
import com.orientasaude.security.BloqueioLoginService;
import com.orientasaude.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AutenticacaoServiceTest {

    private static final String IP = "200.1.2.3";

    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
    private final BloqueioLoginService bloqueioLoginService = mock(BloqueioLoginService.class);
    private final AuditoriaService auditoriaService = mock(AuditoriaService.class);
    private final AutenticacaoService service = new AutenticacaoService(
            usuarioRepository, passwordEncoder, jwtTokenProvider, bloqueioLoginService, auditoriaService);

    @Test
    void registrarComEmailExistenteLancaExcecao() {
        when(usuarioRepository.existsByEmail("maria@exemplo.com")).thenReturn(true);

        assertThatThrownBy(() -> service.registrar(
                new RegistroRequest("Maria", " Maria@Exemplo.com ", "senhaSegura123", true)))
                .isInstanceOf(EmailJaCadastradoException.class);
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void registrarGuardaADataEAVersaoDaPoliticaAceita() {
        when(passwordEncoder.encode("senhaSegura123")).thenReturn("hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocacao -> invocacao.getArgument(0));
        LocalDateTime antes = LocalDateTime.now();

        Usuario salvo = service.registrar(new RegistroRequest("Maria", "maria@exemplo.com", "senhaSegura123", true));

        assertThat(salvo.getConsentimentoEm()).isBetween(antes, LocalDateTime.now());
        assertThat(salvo.getVersaoPolitica()).isEqualTo(AutenticacaoService.VERSAO_POLITICA_PRIVACIDADE);
    }

    @Test
    void loginDeUsuarioInativoLancaCredenciaisInvalidas() {
        Usuario inativo = Usuario.builder().email("maria@exemplo.com").senhaHash("hash").ativo(false).build();
        when(usuarioRepository.findByEmail("maria@exemplo.com")).thenReturn(Optional.of(inativo));

        assertThatThrownBy(() -> service.login(new LoginRequest("maria@exemplo.com", "senhaSegura123"), IP))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void loginComSenhaErradaLancaCredenciaisInvalidas() {
        Usuario usuario = Usuario.builder().id(3L).email("maria@exemplo.com").senhaHash("hash").ativo(true).build();
        when(usuarioRepository.findByEmail("maria@exemplo.com")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("errada123", "hash")).thenReturn(false);

        assertThatThrownBy(() -> service.login(new LoginRequest("maria@exemplo.com", "errada123"), IP))
                .isInstanceOf(BadCredentialsException.class);
        verify(auditoriaService).registrar(TipoEventoAuditoria.LOGIN_FALHO, 3L, IP);
        verify(bloqueioLoginService).registrarFalha(IP);
    }

    @Test
    void loginComEmailInexistenteRegistraFalhaSemUsuario() {
        when(usuarioRepository.findByEmail("ninguem@exemplo.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginRequest("ninguem@exemplo.com", "qualquer123"), IP))
                .isInstanceOf(BadCredentialsException.class);
        verify(auditoriaService).registrar(TipoEventoAuditoria.LOGIN_FALHO, null, IP);
    }

    @Test
    void falhaQueDisparaOBloqueioTambemVaiParaAAuditoria() {
        when(usuarioRepository.findByEmail("ninguem@exemplo.com")).thenReturn(Optional.empty());
        when(bloqueioLoginService.registrarFalha(IP)).thenReturn(true);

        assertThatThrownBy(() -> service.login(new LoginRequest("ninguem@exemplo.com", "qualquer123"), IP))
                .isInstanceOf(BadCredentialsException.class);
        verify(auditoriaService).registrar(TipoEventoAuditoria.LOGIN_BLOQUEADO, null, IP);
    }

    @Test
    void ipBloqueadoNaoEntraNemComASenhaCerta() {
        doThrow(new LoginBloqueadoException(600)).when(bloqueioLoginService).verificar(IP);

        assertThatThrownBy(() -> service.login(new LoginRequest("maria@exemplo.com", "senhaSegura123"), IP))
                .isInstanceOf(LoginBloqueadoException.class)
                .hasMessageContaining("10 minutos");
        verify(usuarioRepository, never()).findByEmail(any());
    }

    @Test
    void loginCertoZeraAsFalhasEColocaOPapelNoToken() {
        Usuario admin = Usuario.builder().id(9L).email("admin@exemplo.com").senhaHash("hash")
                .ativo(true).papel(PapelUsuario.ADMIN).build();
        when(usuarioRepository.findByEmail("admin@exemplo.com")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("senhaSegura123", "hash")).thenReturn(true);
        when(jwtTokenProvider.gerarToken(9L, "admin@exemplo.com", PapelUsuario.ADMIN)).thenReturn("token");

        AutenticacaoResponse resposta = service.login(new LoginRequest("admin@exemplo.com", "senhaSegura123"), IP);

        assertThat(resposta.getToken()).isEqualTo("token");
        verify(bloqueioLoginService).registrarSucesso(IP);
        verify(auditoriaService, never()).registrar(any(), any(), any());
    }

    @Test
    void contaNovaSempreNasceComoUsuario() {
        when(passwordEncoder.encode("senhaSegura123")).thenReturn("hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        Usuario salvo = service.registrar(new RegistroRequest("Maria", "maria@exemplo.com", "senhaSegura123", true));

        assertThat(salvo.getPapel()).isEqualTo(PapelUsuario.USUARIO);
    }
}
