package com.github.stepanterkun.searchengine.search.domain.model;

import com.github.stepanterkun.searchengine.document.domain.model.DocumentStatus;

import java.util.List;

/**
 * Short summary of a document shown in search results.
 */
public record DocumentSummary(
        Long documentId,
        String documentTitle,
        DocumentStatus documentStatus,
        double relevanceScore,
        List<WordContextSnippet> wordSnippets
) {
}
