package com.orientasaude.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Ajusta, ao subir a aplicação, as colunas que passaram a guardar texto cifrado.
 *
 * O texto cifrado em Base64 é maior que o original, e a coluna condicoes_json era do tipo JSON
 * (que não aceita a cifra). O ddl-auto=update do Hibernate cria colunas novas, mas não muda o tipo
 * das que já existem; por isso, nos bancos já criados (homologação e produção), este ajuste troca
 * essas colunas para TEXT. Quando a coluna já é TEXT, nada é feito.
 */
@Component
public class AjusteColunasCriptografadas implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AjusteColunasCriptografadas.class);

    private static final Map<String, List<String>> COLUNAS = Map.of(
            "sessoes_triagem", List.of("sintomas_iniciais"),
            "mensagens_triagem", List.of("conteudo"),
            "resultados_triagem", List.of("condicoes_json", "explicacao_educativa", "orientacoes_gerais",
                    "sinais_alerta", "quando_buscar_atendimento", "nota_limitacao"));

    private static final List<String> TIPOS_DE_TEXTO = List.of("text", "mediumtext", "longtext");

    private final JdbcTemplate jdbcTemplate;

    public AjusteColunasCriptografadas(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        COLUNAS.forEach((tabela, colunas) -> colunas.forEach(coluna -> ajustar(tabela, coluna)));
    }

    private void ajustar(String tabela, String coluna) {
        List<Map<String, Object>> linhas = jdbcTemplate.queryForList("""
                SELECT DATA_TYPE, IS_NULLABLE FROM information_schema.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?
                """, tabela, coluna);
        if (linhas.isEmpty()) {
            return;
        }
        String tipo = String.valueOf(linhas.get(0).get("DATA_TYPE")).toLowerCase();
        if (TIPOS_DE_TEXTO.contains(tipo)) {
            return;
        }
        String nulo = "NO".equals(linhas.get(0).get("IS_NULLABLE")) ? "NOT NULL" : "NULL";
        // Tabela e coluna vêm da lista fixa acima, nunca de entrada do usuário
        jdbcTemplate.execute("ALTER TABLE " + tabela + " MODIFY " + coluna + " TEXT " + nulo);
        log.info("Coluna {}.{} alterada de {} para TEXT para guardar o dado cifrado", tabela, coluna, tipo);
    }
}
