package com.orientasaude.model.enums;

/**
 * Urgency level assigned by the triage.
 * Defines how severe the symptoms are and what the user is advised to do.
 * Constant names stay in Portuguese on purpose: they are the domain values defined in the
 * design (AI response contract, database and UI), not code identifiers.
 */
public enum UrgencyLevel {
    LEVE,
    MODERADO,
    URGENTE,
    EMERGENCIA
}
