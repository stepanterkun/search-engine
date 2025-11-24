package com.github.stepanterkun.searchengine.search.domain.service;

import com.github.stepanterkun.searchengine.search.api.dto.SearchResultDto;
import com.github.stepanterkun.searchengine.search.domain.model.DocumentSummary;
import com.github.stepanterkun.searchengine.search.domain.port.SearchIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Application service for searching documents.
 */
@Service
public class SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    private final SearchIndex searchIndex;

    public SearchService(SearchIndex searchIndex) {
        this.searchIndex = searchIndex;
    }

    /**
     * Searches all documents of a given owner by the query string.
     */
    public SearchResultDto searchAllDocumentsByQuery(Long ownerId, String query) {
        log.debug("Search started: ownerId={}, originalQuery='{}'", ownerId, query);

        List<DocumentSummary> summaries = searchIndex.search(ownerId, query);

        return new SearchResultDto(query, summaries);
    }
}
