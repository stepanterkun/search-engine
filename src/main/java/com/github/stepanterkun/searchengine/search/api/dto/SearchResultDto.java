package com.github.stepanterkun.searchengine.search.api.dto;

import com.github.stepanterkun.searchengine.search.domain.model.DocumentSummary;

import java.util.List;

/**
 * DTO for returning search results.
 */
public record SearchResultDto(
        String originalQuery,
        List<DocumentSummary> documentSummaries
) {
}
