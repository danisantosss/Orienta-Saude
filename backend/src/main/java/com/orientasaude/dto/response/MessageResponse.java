package com.orientasaude.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Generic DTO for responses that carry only a text message.
 * Used for registration confirmations, deletions, etc.
 */
@Data
@AllArgsConstructor
public class MessageResponse {

    private String message;
}
