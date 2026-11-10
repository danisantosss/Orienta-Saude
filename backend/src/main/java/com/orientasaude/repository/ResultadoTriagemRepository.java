package com.orientasaude.repository;

import com.orientasaude.model.ResultadoTriagem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ResultadoTriagemRepository extends JpaRepository<ResultadoTriagem, Long> {

    Optional<ResultadoTriagem> findBySessaoId(Long sessaoId);

    /** Painel: quantos resultados de cada nível, pela data de início da triagem. Cada linha: [NivelUrgencia, Long]. */
    @Query("""
            select r.nivelUrgencia, count(r) from ResultadoTriagem r
            where r.sessao.iniciadaEm >= :inicio and r.sessao.iniciadaEm < :fim
            group by r.nivelUrgencia
            """)
    List<Object[]> contarPorNivelUrgencia(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);
}
