package com.github.stepanterkun.searchengine.search.domain.port;

import com.github.stepanterkun.searchengine.document.domain.model.Document;
import com.github.stepanterkun.searchengine.search.domain.model.DocumentSummary;

import java.util.List;

/**
 * Port for search index.
 * Allows to index documents and search them by text query.
 */
public interface SearchIndex {

    /**
     * Index (or re-index) the given document.
     * Called on document create/update.
     */
    void index(Document document);

    /**
     * Remove document from the index.
     * Called on document delete.
     */
    void remove(Long documentId);

    /**
     * Search documents of a specific owner by text query.
     *
     * @param ownerId id of the owner
     * @param query   free text query
     * @return list of document summaries ordered by relevance
     */
    List<DocumentSummary> search(Long ownerId, String query);
}
