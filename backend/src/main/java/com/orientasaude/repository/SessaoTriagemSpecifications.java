package com.orientasaude.repository;

import com.orientasaude.model.ResultadoTriagem;
import com.orientasaude.model.SessaoTriagem;
import com.orientasaude.model.enums.NivelUrgencia;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

/**
 * Filtros do histórico de triagens. Cada método é uma condição do WHERE; o service
 * junta só as condições dos filtros que o usuário preencheu.
 */
public final class SessaoTriagemSpecifications {

    private SessaoTriagemSpecifications() {
    }

    /**
     * Sessões do usuário. Também traz o resultado na mesma consulta (LEFT JOIN FETCH),
     * para não fazer uma consulta extra por sessão ao ler o nível de urgência.
     */
    public static Specification<SessaoTriagem> doUsuario(Long usuarioId) {
        return (root, query, cb) -> {
            // A consulta de contagem da paginação não pode ter fetch
            if (query != null && !Long.class.equals(query.getResultType())) {
                root.fetch("resultado", JoinType.LEFT);
            }
            return cb.equal(root.get("usuario").get("id"), usuarioId);
        };
    }

    public static Specification<SessaoTriagem> iniciadaAPartirDe(LocalDateTime inicio) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("iniciadaEm"), inicio);
    }

    public static Specification<SessaoTriagem> iniciadaAntesDe(LocalDateTime limite) {
        return (root, query, cb) -> cb.lessThan(root.get("iniciadaEm"), limite);
    }

    /**
     * Feito com subconsulta (EXISTS) porque o resultado já está no fetch de {@link #doUsuario};
     * o Hibernate não aceita outro join na mesma associação.
     */
    public static Specification<SessaoTriagem> comNivelUrgencia(NivelUrgencia nivel) {
        return (root, query, cb) -> {
            Subquery<Long> comNivel = query.subquery(Long.class);
            Root<ResultadoTriagem> resultado = comNivel.from(ResultadoTriagem.class);
            comNivel.select(resultado.get("id"))
                    .where(cb.equal(resultado.get("sessao"), root),
                            cb.equal(resultado.get("nivelUrgencia"), nivel));
            return cb.exists(comNivel);
        };
    }
}
