package com.github.stepanterkun.searchengine.document.domain.port;

import com.github.stepanterkun.searchengine.document.domain.model.Document;
import com.github.stepanterkun.searchengine.document.domain.model.DocumentNotFoundException;

import java.util.List;
import java.util.Optional;

/**
 * Domain-level repository for working with documents.
 * This is a port that hides persistence details from the domain layer.
 */
public interface DocumentRepository {

    Document save(Document document);

    /**
     * Finds a document by id and owner id.
     *
     * @return optional with document or empty if not found
     */
    Optional<Document> findByIdAndOwnerId(Long id, Long ownerId) throws DocumentNotFoundException;

    List<Document> findAllByOwnerId(Long ownerId);

    void deleteDocument(Document doc);

    List<Document> findAll();
}
