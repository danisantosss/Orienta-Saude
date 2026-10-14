package com.orientasaude.model;

import com.orientasaude.model.enums.MessageRole;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity representing a message in the adaptive interview.
 * Can come from the SYSTEM (initial symptoms), the USER (answer) or the ASSISTANT (AI question).
 */
@Entity
@Table(name = "triage_messages", indexes = {
    @Index(name = "idx_triage_messages_session", columnList = "session_id, sequence_order")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TriageMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private TriageSession session;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private MessageRole role;

    @NotBlank
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "sequence_order", nullable = false)
    private Integer sequenceOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
