package com.orientasaude.model;

import com.orientasaude.model.enums.UrgencyLevel;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity storing the final result of a triage.
 * Holds the urgency classification, possible conditions, guidance and disclaimer.
 */
@Entity
@Table(name = "triage_results")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TriageResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false, unique = true)
    private TriageSession session;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "urgency_level", nullable = false, length = 15)
    private UrgencyLevel urgencyLevel;

    @NotBlank
    @Column(name = "conditions_json", nullable = false, columnDefinition = "JSON")
    private String conditionsJson;

    @NotBlank
    @Column(name = "educational_explanation", nullable = false, columnDefinition = "TEXT")
    private String educationalExplanation;

    @NotBlank
    @Column(name = "general_guidance", nullable = false, columnDefinition = "TEXT")
    private String generalGuidance;

    @NotBlank
    @Column(name = "warning_signs", nullable = false, columnDefinition = "TEXT")
    private String warningSigns;

    @NotBlank
    @Column(name = "when_to_seek_care", nullable = false, columnDefinition = "TEXT")
    private String whenToSeekCare;

    @NotBlank
    @Column(nullable = false, columnDefinition = "TEXT")
    private String disclaimer;

    @Column(name = "limitations_note", columnDefinition = "TEXT")
    private String limitationsNote;

    @Column(name = "rag_documents_used", columnDefinition = "JSON")
    private String ragDocumentsUsed;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
