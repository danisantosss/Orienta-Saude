package com.orientasaude.config;

import com.orientasaude.model.Usuario;
import com.orientasaude.model.enums.PapelUsuario;
import com.orientasaude.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdministradorInicialTest {

    private final UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);

    @Test
    void promoveAsContasDaVariavel() {
        Usuario usuario = Usuario.builder().id(1L).email("chefe@exemplo.com").build();
        when(usuarioRepository.findByEmail("chefe@exemplo.com")).thenReturn(Optional.of(usuario));
        when(usuarioRepository.findByEmail("naoexiste@exemplo.com")).thenReturn(Optional.empty());

        new AdministradorInicial(usuarioRepository, " Chefe@Exemplo.com , naoexiste@exemplo.com").run(null);

        assertThat(usuario.getPapel()).isEqualTo(PapelUsuario.ADMIN);
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void contaQueJaEhAdminNaoEhGravadaDeNovo() {
        Usuario admin = Usuario.builder().id(1L).email("chefe@exemplo.com").papel(PapelUsuario.ADMIN).build();
        when(usuarioRepository.findByEmail("chefe@exemplo.com")).thenReturn(Optional.of(admin));

        new AdministradorInicial(usuarioRepository, "chefe@exemplo.com").run(null);

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void semVariavelNaoFazNada() {
        new AdministradorInicial(usuarioRepository, "").run(null);

        verify(usuarioRepository, never()).findByEmail(anyString());
    }
}
