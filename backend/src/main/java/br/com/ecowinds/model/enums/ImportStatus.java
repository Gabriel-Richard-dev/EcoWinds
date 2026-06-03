package br.com.ecowinds.model.enums;

public enum ImportStatus {
    PENDING,
    SUCCESS,
    SKIPPED_DUPLICATE,
    PARSE_ERROR,
    VALIDATION_ERROR,
    PERSISTENCE_ERROR
}
