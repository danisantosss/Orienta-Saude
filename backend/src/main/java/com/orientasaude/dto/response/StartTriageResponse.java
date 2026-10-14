package com.orientasaude.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StartTriageResponse {
    private Long sessionId;
    private String firstQuestion;
}
