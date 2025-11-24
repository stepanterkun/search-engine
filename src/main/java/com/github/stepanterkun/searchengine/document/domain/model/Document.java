package com.github.stepanterkun.searchengine.document.domain.model;

public class Document {

    private Long id;
    private String title;
    private String content;
    private Long ownerId;
    private DocumentStatus status;

    public static Document newDocument(String title, String content, Long ownerId) {
        return new Document(null, title, content, ownerId, DocumentStatus.NEW);
    }

    public Document(Long id, String title, String content, Long ownerId, DocumentStatus status) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.ownerId = ownerId;
        this.status = status;
    }

    public void markIndexing() {
        this.status = DocumentStatus.INDEXING;
    }

    public void markReady() {
        this.status = DocumentStatus.READY;
    }

    public void markFailed() {
        this.status = DocumentStatus.FAILED;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public void setStatus(DocumentStatus status) {
        this.status = status;
    }
}
