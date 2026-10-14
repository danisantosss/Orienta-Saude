package com.orientasaude.model.enums;

/**
 * Role of whoever sent a message in the adaptive interview.
 * SYSTEM: initial message with the symptoms
 * USER: patient's answer
 * ASSISTANT: AI-generated question
 */
public enum MessageRole {
    SYSTEM,
    USER,
    ASSISTANT
}
