package com.orientasaude.repository;

import com.orientasaude.model.SessaoTriagem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SessaoTriagemRepository extends JpaRepository<SessaoTriagem, Long>,
        JpaSpecificationExecutor<SessaoTriagem> {

    /** Painel: quantas triagens começaram em cada dia do período. Cada linha: [LocalDate, Long]. */
    @Query("""
            select cast(s.iniciadaEm as LocalDate), count(s) from SessaoTriagem s
            where s.iniciadaEm >= :inicio and s.iniciadaEm < :fim
            group by cast(s.iniciadaEm as LocalDate)
            """)
    List<Object[]> contarPorDia(@Param("inicio") LocalDateTime inicio, @Param("fim") LocalDateTime fim);

    /** Painel: quantas entrevistas terminaram por cada motivo. Cada linha: [MotivoEncerramento, Long]. */
    @Query("""
            select s.motivoEncerramento, count(s) from SessaoTriagem s
            where s.iniciadaEm >= :inicio and s.iniciadaEm < :fim and s.motivoEncerramento is not null
            group by s.motivoEncerramento
            """)
    List<Object[]> contarPorMotivoEncerramento(@Param("inicio") LocalDateTime inicio,
                                               @Param("fim") LocalDateTime fim);
}
