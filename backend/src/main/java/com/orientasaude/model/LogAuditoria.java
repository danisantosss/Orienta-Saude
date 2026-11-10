package com.orientasaude.model;

import com.orientasaude.model.enums.TipoEventoAuditoria;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Um evento de segurança. Guarda só quem (id, quando houver), de onde (IP), o quê (tipo) e quando.
 * Não há relacionamento com Usuario de propósito: o registro precisa continuar existindo
 * depois que a conta é excluída (aí o usuarioId passa a ser nulo).
 */
@Entity
@Table(name = "logs_auditoria", indexes = {
    @Index(name = "idx_auditoria_data", columnList = "criado_em")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(nullable = false, length = 45)
    private String ip;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_evento", nullable = false, length = 40)
    private TipoEventoAuditoria tipoEvento;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    protected void aoSerCriado() {
        if (criadoEm == null) {
            criadoEm = LocalDateTime.now();
        }
    }
}
