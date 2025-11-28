package com.github.stepanterkun.searchengine.search.api.dto;

import com.github.stepanterkun.searchengine.search.domain.model.DocumentSummary;

import java.util.List;

/**
 * DTO for returning search results.
 */
public record SearchResultDto(
        String originalQuery,
        int page, // 1-based indexation
        int size,
        long totalElements,
        int totalPages,
        boolean hasPrevious,
        boolean hasNext,
        List<DocumentSummary> documentSummaries
        ) {
}
