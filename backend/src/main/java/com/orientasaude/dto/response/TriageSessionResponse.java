package com.orientasaude.dto.response;

import com.orientasaude.model.enums.SessionStatus;
import com.orientasaude.model.enums.UrgencyLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
public class TriageSessionResponse {
    private Long id;
    private String initialSymptoms;
    private Integer patientAge;
    private SessionStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private UrgencyLevel urgencyLevel;
}
