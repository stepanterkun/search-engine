package com.github.stepanterkun.searchengine.search.domain.service;

import com.github.stepanterkun.searchengine.search.api.dto.SearchResultDto;
import com.github.stepanterkun.searchengine.search.domain.model.DocumentSummary;
import com.github.stepanterkun.searchengine.search.domain.port.SearchIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

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
    public SearchResultDto searchAllDocumentsByQuery(Long ownerId, String query, Integer pageNumber, Integer pageSize) {
        log.debug("Search started: ownerId={}, originalQuery='{}', pageNumber={}, pageSize={}",
                ownerId, query, pageNumber, pageSize);

        pageNumber = (pageNumber == null || pageNumber < 1) ? PAGE_DEFAULT : pageNumber;
        pageSize = (pageSize == null || pageSize < 1) ? SIZE_DEFAULT : pageSize;

        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize);
        Page<DocumentSummary> page = searchIndex.search(ownerId, query, pageable);

        long totalElements = page.getTotalElements();
        int totalPages = page.getTotalPages();

        if (totalElements == 0L) {
            return new SearchResultDto(
                    query,
                    pageNumber,
                    pageSize,
                    0L,
                    0,
                    false,
                    false,
                    List.of()
            );
        }

        if (pageNumber > totalPages) {
            pageNumber = totalPages;
            pageable = PageRequest.of(pageNumber - 1, pageSize);
            page = searchIndex.search(ownerId, query, pageable);
        }

        boolean hasPrevious = page.hasPrevious();
        boolean hasNext = page.hasNext();

        List<DocumentSummary> pageContent = page.getContent();

        return new SearchResultDto(
                query,
                pageNumber,
                pageSize,
                totalElements,
                totalPages,
                hasPrevious,
                hasNext,
                pageContent
        );
    }
}
