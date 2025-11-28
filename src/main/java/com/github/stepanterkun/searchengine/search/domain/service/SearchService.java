package com.github.stepanterkun.searchengine.search.domain.service;

import com.github.stepanterkun.searchengine.search.api.dto.SearchResultDto;
import com.github.stepanterkun.searchengine.search.domain.model.DocumentSummary;
import com.github.stepanterkun.searchengine.search.domain.port.SearchIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Application service for searching documents.
 */
@Service
public class SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    private static final int PAGE_DEFAULT = 1;
    private static final int SIZE_DEFAULT = 20;

    private final SearchIndex searchIndex;

    public SearchService(SearchIndex searchIndex) {
        this.searchIndex = searchIndex;
    }

    /**
     * Searches all documents of a given owner by the query string.
     */
    public SearchResultDto searchAllDocumentsByQuery(Long ownerId, String query, Integer page, Integer size) {
        log.debug("Search started: ownerId={}, originalQuery='{}', page={}, size={}", ownerId, query, page, size);

        page = (page == null || page < 1) ? PAGE_DEFAULT : page;
        size = (size == null || size < 1) ? SIZE_DEFAULT : size;

        List<DocumentSummary> allSummaries = searchIndex.search(ownerId, query);
        int totalElements = allSummaries.size();

        if (totalElements == 0) {
            return new SearchResultDto(
                    query,
                    page,
                    size,
                    0L,
                    0,
                    false,
                    false,
                    List.of()
                    );
        }

        int totalPages = (int) Math.ceil((double) totalElements / size);

        int effectivePage = Math.min(page, totalPages);

        int fromIndex = (effectivePage - 1) * size;
        int toIndex = Math.min(fromIndex + size, totalElements);

        List<DocumentSummary> pageContent = allSummaries.subList(fromIndex, toIndex);

        boolean hasPrevious = effectivePage > 1;
        boolean hasNext = effectivePage < totalPages;

        return new SearchResultDto(
                query,
                effectivePage,
                size,
                totalElements,
                totalPages,
                hasPrevious,
                hasNext,
                pageContent
        );
    }
}
