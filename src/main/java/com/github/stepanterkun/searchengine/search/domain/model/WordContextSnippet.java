package com.github.stepanterkun.searchengine.search.domain.model;

import java.util.List;

/**
 * Snippets of text around a matched term in a document.
 */
public record WordContextSnippet(
        String term,
        List<String> snippets
) { }
