package com.orientasaude.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TriageMessageResponse {
    private String question;

    @JsonProperty("isComplete")
    private boolean complete;
}
