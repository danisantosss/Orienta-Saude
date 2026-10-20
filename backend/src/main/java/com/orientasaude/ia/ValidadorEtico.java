package com.orientasaude.ia;

import com.orientasaude.model.enums.NivelUrgencia;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Confere a resposta da IA antes de ela ser gravada ou exibida (Propriedades 2 e 4):
 * nada de certeza diagnóstica, nada de prescrição e nível de urgência sempre válido.
 * O texto é comparado em minúsculas e sem acentos, para "Você tem" e "VOCE TEM" serem a mesma coisa.
 */
@Component
public class ValidadorEtico {

    public static final String DISCLAIMER =
            "Estas orientações são exclusivamente educativas, não constituem diagnóstico médico e não substituem "
                    + "a avaliação de um profissional de saúde. Procure atendimento presencial para confirmar qualquer suspeita. "
                    + "Em caso de emergência, ligue para o SAMU (192).";

    private static final Logger log = LoggerFactory.getLogger(ValidadorEtico.class);

    private static final List<Pattern> CERTEZA_DIAGNOSTICA = List.of(
            Pattern.compile("\\bvoce tem\\b"),
            Pattern.compile("\\bdiagnostico confirmado\\b"),
            Pattern.compile("\\bvoce sofre de\\b"),
            Pattern.compile("\\bfoi diagnosticad[oa]\\b"),
            Pattern.compile("\\bdiagnosticad[oa] com\\b"));

    private static final List<String> MEDICAMENTOS = List.of(
            "paracetamol", "dipirona", "ibuprofeno", "acido acetilsalicilico", "aspirina", "amoxicilina",
            "azitromicina", "cefalexina", "ciprofloxacino", "dexametasona", "prednisona", "prednisolona",
            "omeprazol", "diclofenaco", "nimesulida", "loratadina", "dexclorfeniramina", "salbutamol",
            "butilescopolamina", "buscopan", "dorflex", "tylenol", "novalgina", "ivermectina", "cloroquina");

    private static final List<Pattern> PRESCRICAO = criarPadroesDePrescricao();

    private static List<Pattern> criarPadroesDePrescricao() {
        List<Pattern> padroes = new ArrayList<>(List.of(
                Pattern.compile("\\b\\d+([.,]\\d+)?\\s?(mg|mcg)\\b"),
                Pattern.compile("\\bcomprimidos?\\b"),
                Pattern.compile("\\bcapsulas?\\b"),
                Pattern.compile("\\bde \\d+ em \\d+ horas\\b"),
                Pattern.compile("\\b\\d+ gotas\\b")));
        MEDICAMENTOS.forEach(nome -> padroes.add(Pattern.compile("\\b" + nome + "\\b")));
        return List.copyOf(padroes);
    }

    /**
     * Devolve os trechos proibidos encontrados em todos os campos de texto (lista vazia = aprovado).
     */
    public List<String> validar(AnaliseGerada analise) {
        List<String> textos = new ArrayList<>();
        textos.addAll(analise.condicoes());
        textos.add(analise.explicacaoEducativa());
        textos.add(analise.orientacoesGerais());
        textos.addAll(analise.sinaisAlerta());
        textos.add(analise.quandoBuscarAtendimento());

        Set<String> encontrados = new LinkedHashSet<>();
        for (String texto : textos) {
            String normalizado = normalizar(texto);
            procurar(normalizado, CERTEZA_DIAGNOSTICA, encontrados);
            procurar(normalizado, PRESCRICAO, encontrados);
        }
        return List.copyOf(encontrados);
    }

    /**
     * Converte o nível vindo da IA. Qualquer valor fora dos quatro níveis vira URGENTE,
     * o mais prudente sem alarmar como emergência, e fica registrado no log (Propriedade 2).
     */
    public NivelUrgencia interpretarNivel(String valor) {
        String normalizado = valor == null ? "" : normalizar(valor).trim().toUpperCase();
        try {
            return NivelUrgencia.valueOf(normalizado);
        } catch (IllegalArgumentException e) {
            log.warn("Nível de urgência inválido devolvido pela IA ({}); usando URGENTE", valor);
            return NivelUrgencia.URGENTE;
        }
    }

    private void procurar(String texto, List<Pattern> padroes, Set<String> encontrados) {
        for (Pattern padrao : padroes) {
            Matcher matcher = padrao.matcher(texto);
            if (matcher.find()) {
                encontrados.add(matcher.group());
            }
        }
    }

    private static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase();
    }
}
