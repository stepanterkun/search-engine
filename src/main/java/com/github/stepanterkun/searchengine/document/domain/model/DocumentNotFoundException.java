package com.github.stepanterkun.searchengine.document.domain.model;

import jakarta.persistence.EntityNotFoundException;

public class DocumentNotFoundException extends EntityNotFoundException {

    private final Long id;

    public DocumentNotFoundException(String message) {
        super(message);
        this.id = null;
    }

    public DocumentNotFoundException(Long id) {
        super("Document not found with id=" + id);
        this.id = id;
    }

    public DocumentNotFoundException(String message, Long id) {
        super(message);
        this.id = id;
    }

    public DocumentNotFoundException(String message, Long id, Throwable cause) {
        super(message, (Exception) cause);
        this.id = id;
    }

    public Long getDocumentId() {
        return id;
    }
}
