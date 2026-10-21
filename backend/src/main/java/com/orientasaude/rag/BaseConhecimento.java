package com.orientasaude.rag;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.orientasaude.config.RagConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Carrega os documentos da base de conhecimento quando a aplicação sobe.
 * Um documento inválido (sem fonte, sem conteúdo, id repetido...) impede a aplicação de subir:
 * é melhor falhar no deploy do que dar orientação sem fonte.
 */
@Component
public class BaseConhecimento {

    private static final Logger log = LoggerFactory.getLogger(BaseConhecimento.class);
    private static final List<String> NIVEIS = List.of("leve", "moderado", "urgente", "emergencia");

    private final List<DocumentoConhecimento> documentos;
    private final Map<String, DocumentoConhecimento> porId = new HashMap<>();

    public BaseConhecimento(RagConfig config, ObjectMapper objectMapper) {
        ObjectMapper leitor = objectMapper.copy().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        List<DocumentoConhecimento> carregados = new ArrayList<>();
        for (Resource arquivo : listarArquivos(config.getPadraoDocumentos())) {
            DocumentoConhecimento documento = ler(leitor, arquivo);
            validar(documento, arquivo.getFilename());
            if (porId.putIfAbsent(documento.id(), documento) != null) {
                throw new IllegalStateException("Id repetido na base de conhecimento: " + documento.id());
            }
            carregados.add(documento);
        }
        this.documentos = List.copyOf(carregados);
        log.info("Base de conhecimento carregada com {} documentos", documentos.size());
    }

    public List<DocumentoConhecimento> listar() {
        return documentos;
    }

    public Optional<DocumentoConhecimento> buscarPorId(String id) {
        return Optional.ofNullable(porId.get(id));
    }

    private Resource[] listarArquivos(String padrao) {
        try {
            Resource[] arquivos = new PathMatchingResourcePatternResolver().getResources(padrao);
            if (arquivos.length == 0) {
                throw new IllegalStateException("Nenhum documento encontrado em " + padrao);
            }
            return arquivos;
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível ler a base de conhecimento em " + padrao, e);
        }
    }

    private DocumentoConhecimento ler(ObjectMapper leitor, Resource arquivo) {
        try (InputStream entrada = arquivo.getInputStream()) {
            return leitor.readValue(entrada, DocumentoConhecimento.class);
        } catch (IOException e) {
            throw new IllegalStateException("Documento inválido: " + arquivo.getFilename(), e);
        }
    }

    private void validar(DocumentoConhecimento documento, String arquivo) {
        List<String> problemas = new ArrayList<>();
        exigirTexto(documento.id(), "id", problemas);
        exigirTexto(documento.sintoma(), "sintoma", problemas);
        exigirTexto(documento.fonte(), "fonte", problemas);
        exigirTexto(documento.conteudo(), "conteudo", problemas);
        exigirTexto(documento.orientacoesGerais(), "orientacoesGerais", problemas);
        if (documento.urlFonte() == null || !documento.urlFonte().startsWith("https://")) {
            problemas.add("urlFonte");
        }
        exigirLista(documento.palavrasChave(), "palavrasChave", problemas);
        exigirLista(documento.sinaisAlerta(), "sinaisAlerta", problemas);
        exigirLista(documento.condicoesRelacionadas(), "condicoesRelacionadas", problemas);
        if (documento.indicadoresUrgencia() == null || !documento.indicadoresUrgencia().keySet().containsAll(NIVEIS)) {
            problemas.add("indicadoresUrgencia");
        }
        if (!problemas.isEmpty()) {
            throw new IllegalStateException("Documento " + arquivo + " com campos inválidos: " + problemas);
        }
    }

    private void exigirTexto(String valor, String campo, List<String> problemas) {
        if (valor == null || valor.isBlank()) {
            problemas.add(campo);
        }
    }

    private void exigirLista(Collection<String> valor, String campo, List<String> problemas) {
        if (valor == null || valor.isEmpty()) {
            problemas.add(campo);
        }
    }
}
