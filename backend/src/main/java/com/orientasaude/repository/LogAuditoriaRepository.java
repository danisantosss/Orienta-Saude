package com.orientasaude.repository;

import com.orientasaude.model.LogAuditoria;
import com.orientasaude.model.enums.TipoEventoAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LogAuditoriaRepository extends JpaRepository<LogAuditoria, Long> {

    List<LogAuditoria> findByTipoEventoAndIp(TipoEventoAuditoria tipoEvento, String ip);

    List<LogAuditoria> findByUsuarioId(Long usuarioId);

    /** Desliga os registros da conta excluída: o evento continua, mas sem dizer de quem era. */
    @Modifying
    @Query("update LogAuditoria l set l.usuarioId = null where l.usuarioId = :usuarioId")
    int anonimizarUsuario(@Param("usuarioId") Long usuarioId);

    @Modifying
    @Query("delete from LogAuditoria l where l.criadoEm < :limite")
    int removerAnterioresA(@Param("limite") LocalDateTime limite);
}
