package com.github.stepanterkun.searchengine.search.api.controller;

import com.github.stepanterkun.searchengine.search.api.dto.SearchResultDto;
import com.github.stepanterkun.searchengine.search.domain.service.SearchService;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for searching documents of the current user.
 */
@RestController
@RequestMapping("/documents/search")
public class SearchController {

    private static final Logger log = LoggerFactory.getLogger(SearchController.class);

    private final SearchService service;

    public SearchController(SearchService service) {
        this.service = service;
    }

    /**
     * Searches all documents of the given owner by text query.
     */
    @GetMapping("/all")
    public ResponseEntity<SearchResultDto> searchAllDocuments(
            @RequestHeader("X-User-Id") @NotNull Long ownerId,
            @RequestParam("query") @NotNull String query
    ) {
        log.info("Search documents: ownerId={}, query='{}'", ownerId, query);

        SearchResultDto result = service.searchAllDocumentsByQuery(ownerId, query);
        return ResponseEntity.ok(result);
    }
}
