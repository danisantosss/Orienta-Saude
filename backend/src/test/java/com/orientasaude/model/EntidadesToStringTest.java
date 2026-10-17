package com.orientasaude.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O Spring Security chama toString() no usuário autenticado. Se o toString percorresse
 * as coleções lazy, toda requisição autenticada falharia fora da sessão do Hibernate;
 * e o hash da senha não pode aparecer em logs.
 */
class EntidadesToStringTest {

    @Test
    void toStringDoUsuarioNaoExpoeSenhaNemPercorreSessoes() {
        Usuario usuario = Usuario.builder().id(1L).nome("Ana").email("ana@exemplo.com").senhaHash("hash-secreto").build();
        SessaoTriagem sessao = SessaoTriagem.builder().id(5L).usuario(usuario).build();
        usuario.setSessoes(List.of(sessao));

        assertThat(usuario.toString()).contains("ana@exemplo.com").doesNotContain("hash-secreto").doesNotContain("sessoes");
    }

    @Test
    void toStringDaSessaoEDaMensagemNaoPercorremRelacionamentos() {
        Usuario usuario = Usuario.builder().id(1L).nome("Ana").build();
        SessaoTriagem sessao = SessaoTriagem.builder().id(5L).usuario(usuario).build();
        MensagemTriagem mensagem = MensagemTriagem.builder().id(9L).sessao(sessao).conteudo("Oi").build();
        sessao.setMensagens(List.of(mensagem));

        assertThat(sessao.toString()).doesNotContain("Ana").doesNotContain("mensagens");
        assertThat(mensagem.toString()).doesNotContain("sessao=");
        assertThat(sessao.hashCode()).isEqualTo(sessao.hashCode());
    }
}
