package com.orientasaude.model;

import com.orientasaude.model.enums.PapelMensagem;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MensagemTriagemTest {

    @Test
    void somenteMensagensDoAssistenteContamComoPerguntaDaEntrevista() {
        assertThat(MensagemTriagem.builder().papel(PapelMensagem.ASSISTENTE).build().isPerguntaDaEntrevista()).isTrue();
        assertThat(MensagemTriagem.builder().papel(PapelMensagem.USUARIO).build().isPerguntaDaEntrevista()).isFalse();
        assertThat(MensagemTriagem.builder().papel(PapelMensagem.SISTEMA).build().isPerguntaDaEntrevista()).isFalse();
    }
}
