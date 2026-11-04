package com.orientasaude.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orientasaude.config.RagConfig;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Carga e validação dos documentos da base de conhecimento.
 */
class BaseConhecimentoTest {

    private static final List<String> DOMINIOS_OFICIAIS = List.of("gov.br", "fiocruz.br", "who.int");

    private BaseConhecimento carregar(String padrao) {
        RagConfig config = new RagConfig();
        config.setPadraoDocumentos(padrao);
        return new BaseConhecimento(config, new ObjectMapper());
    }

    @Test
    void baseRealTemPeloMenos15SintomasDiferentesComFonteOficial() {
        BaseConhecimento base = carregar("classpath:knowledge-base/*.json");
        List<DocumentoConhecimento> documentos = base.listar();

        Set<String> sintomas = documentos.stream().map(DocumentoConhecimento::sintoma).collect(Collectors.toSet());
        assertThat(sintomas).hasSizeGreaterThanOrEqualTo(15);
        assertThat(documentos).allSatisfy(documento -> {
            assertThat(documento.fonte()).isNotBlank();
            assertThat(documento.urlFonte()).startsWith("https://");
            assertThat(documento.conteudo()).isNotBlank();
            assertThat(documento.palavrasChave()).isNotEmpty();
            assertThat(documento.sinaisAlerta()).isNotEmpty();
            assertThat(documento.indicadoresUrgencia()).containsKeys("leve", "moderado", "urgente", "emergencia");
        });
    }

    /**
     * Confere só o formato do link, sem acessar a internet: endereço https bem formado, de um domínio
     * oficial de saúde e sem caracteres que apps de mensagem costumam cortar ao transformar em link.
     */
    @Test
    void urlFonteDeTodosOsDocumentosEhUmLinkOficialBemFormado() {
        List<DocumentoConhecimento> documentos = carregar("classpath:knowledge-base/*.json").listar();

        assertThat(documentos).allSatisfy(documento -> {
            String url = documento.urlFonte();
            assertThat(url).as(documento.id()).doesNotContainAnyWhitespaces().doesNotContain("(", ")");

            URI endereco = URI.create(url);
            assertThat(endereco.getScheme()).as(documento.id()).isEqualTo("https");
            assertThat(endereco.getHost()).as(documento.id()).isNotNull();
            assertThat(DOMINIOS_OFICIAIS).as(documento.id())
                    .anyMatch(dominio -> endereco.getHost().equals(dominio) || endereco.getHost().endsWith("." + dominio));
            assertThat(endereco.getPath()).as(documento.id()).isNotBlank().isNotEqualTo("/");
        });
    }

    @Test
    void idsDaBaseRealSaoUnicos() {
        List<DocumentoConhecimento> documentos = carregar("classpath:knowledge-base/*.json").listar();

        assertThat(documentos).extracting(DocumentoConhecimento::id).doesNotHaveDuplicates();
    }

    @Test
    void buscaDocumentoPorId() {
        BaseConhecimento base = carregar("classpath:knowledge-base-teste/valida/*.json");

        assertThat(base.buscarPorId("doc-a")).get().extracting(DocumentoConhecimento::fonte).isEqualTo("Fonte A");
        assertThat(base.buscarPorId("inexistente")).isEmpty();
    }

    @Test
    void textoIndexavelJuntaSintomaConteudoEPalavrasChave() {
        DocumentoConhecimento documento = carregar("classpath:knowledge-base-teste/valida/*.json").listar().get(0);

        assertThat(documento.textoIndexavel()).contains("febre").contains("Conteúdo A");
    }

    @Test
    void documentoSemFonteImpedeACarga() {
        assertThatThrownBy(() -> carregar("classpath:knowledge-base-teste/invalida/*.json"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("sem-fonte");
    }

    @Test
    void pastaSemDocumentosImpedeACarga() {
        assertThatThrownBy(() -> carregar("classpath:pasta-que-nao-existe/*.json"))
                .isInstanceOf(IllegalStateException.class);
    }
}
