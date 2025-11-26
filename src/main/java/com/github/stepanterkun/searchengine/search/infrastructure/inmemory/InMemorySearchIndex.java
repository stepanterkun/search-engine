package com.github.stepanterkun.searchengine.search.infrastructure.inmemory;

import com.github.stepanterkun.searchengine.document.domain.model.Document;
import com.github.stepanterkun.searchengine.document.domain.model.DocumentNotFoundException;
import com.github.stepanterkun.searchengine.document.domain.model.DocumentStatus;
import com.github.stepanterkun.searchengine.document.domain.port.DocumentRepository;
import com.github.stepanterkun.searchengine.search.domain.model.DocumentSummary;
import com.github.stepanterkun.searchengine.search.domain.model.WordContextSnippet;
import com.github.stepanterkun.searchengine.search.domain.port.SearchIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of {@link SearchIndex} using inverted indexContent.
 */
@Component
public class InMemorySearchIndex implements SearchIndex {

    private static final Logger log = LoggerFactory.getLogger(InMemorySearchIndex.class);

    private final DocumentRepository repository;

    // term -> (documentId -> TermStats(title and content frequencies) )
    private final Map<String, Map<Long, TermStats>> index = new ConcurrentHashMap<>();

    // documentId -> ownerId
    private final Map<Long, Long> owners = new ConcurrentHashMap<>();

    public InMemorySearchIndex(DocumentRepository documentRepository) {
        this.repository = documentRepository;
    }

    private static final class TermStats {
        private int titleFreq;
        private int contentFreq;

        void incTitle() {
            titleFreq++;
        }

        void incContent() {
            contentFreq++;
        }

        int getTitleFreq() {
            return titleFreq;
        }

        int getContentFreq() {
            return contentFreq;
        }

        /**
         * Calculates term frequency (TF) with an extra boost for the title.
         */
        double tf(double titleBoost) {
            return contentFreq + titleBoost * titleFreq;
        }

        boolean isEmpty() {
            return contentFreq == 0 && titleFreq == 0;
        }
    }


    /**
     * Build indexContent for all documents on application startup.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void buildIndexOnStartup() {
        log.info("Indexing all documents in database on startup");

        List<Document> allDocs = repository.findAll();

        allDocs.stream()
                .filter(doc -> doc.getStatus() != DocumentStatus.FAILED)
                .forEach(this::index);
    }

    @Override
    public List<DocumentSummary> search(Long ownerId, String query) {
        log.debug("Search documents: ownerId={}, originalQuery={}", ownerId, query);

        // docId -> total score (sum of term frequencies)
        Map<Long, Double> scores = new HashMap<>();

        final int TITLE_BOOST = 3;

        String normalizedQuery = query == null ? "" : query.toLowerCase().trim();
        if (normalizedQuery.isEmpty()) { return List.of();}
        String[] tokens = normalizedQuery.split("\\W+");

        for (String token : tokens) {
            if (token.isBlank()) continue;

            Map<Long, TermStats> docsIdsAndOccurrences = index.get(token);
            if (docsIdsAndOccurrences == null) continue;


            for (Map.Entry<Long, TermStats> entry : docsIdsAndOccurrences.entrySet()) {
                Long docId = entry.getKey();
                Long owner = owners.get(docId);
                if (owner == null || !owner.equals(ownerId)) continue;

                TermStats stats = entry.getValue();
                double tf = stats.tf(TITLE_BOOST);
                double idf = 1; // todo: implement idf feature
                double termScore = tf * idf;

                scores.merge(docId, termScore, Double::sum);
            }
        }

        // sort documents by score descending
        List<Long> sortedDocIds = scores.entrySet()
                                          .stream()
                                          .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                                          .map(Map.Entry::getKey)
                                          .toList();

        // build summaries for each document
        return sortedDocIds
                       .stream()
                       .map(id -> {
                           Document doc = repository
                                                  .findByIdAndOwnerId(id, ownerId)
                                                  .orElseThrow(() -> new DocumentNotFoundException(id));

                           return new DocumentSummary(
                                   id,
                                   doc.getTitle(),
                                   doc.getStatus(),
                                   scores.get(id),
                                   buildWordSnippets(doc, tokens)
                           );
                       })
                       .toList();
    }

    @Override
    public void index(Document document) {
        log.debug("Indexing document: id={}, ownerId={}", document.getId(), document.getOwnerId());

        Long docId = document.getId();
        if (docId == null) {
            throw new IllegalArgumentException("Cannot index document with null id");
        }

        owners.put(docId, document.getOwnerId());
        removeFromIndexOnly(docId);

        String title = document.getTitle();
        if (title == null || title.isBlank()) {
            throw new IllegalStateException("Cannot index document because of empty title.");
        }
        String[] titleTokens = title.toLowerCase().split("\\W+");

        String content = document.getContent();
        if (content == null || content.isBlank()) {
            throw new IllegalStateException("Cannot index document because of empty content.");
        }
        String[] contentTokens = content.toLowerCase().split("\\W+");

        for (String titleToken : titleTokens) {
            if (titleToken.isBlank()) {
                continue;
            }

            index
                    .computeIfAbsent(titleToken, t -> new ConcurrentHashMap<>())
                    .computeIfAbsent(docId, id -> new TermStats())
                    .incTitle();
        }

        for (String contentToken : contentTokens) {
            if (contentToken.isBlank()) {
                continue;
            }

            index
                    .computeIfAbsent(contentToken, t -> new ConcurrentHashMap<>())
                    .computeIfAbsent(docId, id -> new TermStats())
                    .incContent();
        }
    }

    @Override
    public void remove(Long documentId) {
        log.debug("Remove document from indexContent: id={}", documentId);

        if (documentId == null) {
            return;
        }

        // remove owner mapping
        owners.remove(documentId);
        // remove document from all term maps
        removeFromIndexOnly(documentId);
    }

    private void removeFromIndexOnly(Long documentId) {
        // iterate over all terms and remove this document id
        for (Map<Long, TermStats> docsAndOccurrences : index.values()) {
            docsAndOccurrences.remove(documentId);
        }
    }

    private List<WordContextSnippet> buildWordSnippets(Document doc, String[] queryTokens) {
        String content = doc.getContent();
        if (content == null || content.isBlank()) {
            return List.of();
        }

        String lowerContent = content.toLowerCase();

        // take unique non-blank tokens from query
        List<String> uniqueTokens = Arrays.stream(queryTokens)
                                            .map(String::toLowerCase)
                                            .filter(token -> !token.isBlank())
                                            .distinct()
                                            .toList();

        int contextSize = 30;       // characters before and after the matched term
        int maxSnippetsPerWord = 2; // max number of snippets per term

        List<WordContextSnippet> result = new ArrayList<>();

        for (String token : uniqueTokens) {
            String lowerToken = token.toLowerCase();

            List<String> snippets = new ArrayList<>();
            int fromIndex = 0;

            while (snippets.size() < maxSnippetsPerWord) {
                int idx = lowerContent.indexOf(lowerToken, fromIndex);
                if (idx == -1) {
                    break;
                }

                int roughStart = Math.max(0, idx - contextSize);
                int roughEnd = Math.min(content.length(), idx + lowerToken.length() + contextSize);

                // adjust boundaries to sentence borders if possible
                int adjustedStart = adjustStartToSentenceBoundary(content, roughStart, idx);
                int adjustedEnd = adjustEndToSentenceBoundary(content, roughEnd, idx + lowerToken.length());

                int start = Math.max(0, Math.min(adjustedStart, content.length()));
                int end = Math.max(start, Math.min(adjustedEnd, content.length()));

                String snippet = content.substring(start, end);

                boolean hasPrefix = start > 0;
                boolean hasSuffix = end < content.length();

                if (hasPrefix) {
                    snippet = "... " + snippet;
                }
                if (hasSuffix) {
                    snippet = snippet + " ...";
                }

                snippet = snippet.trim();
                snippets.add(snippet);

                // move search window forward to find next occurrence
                fromIndex = idx + lowerToken.length();
            }

            if (!snippets.isEmpty()) {
                result.add(new WordContextSnippet(token, snippets));
            }
        }

        return result;
    }

    private int adjustStartToSentenceBoundary(String text, int roughStart, int tokenIndex) {
        // search backwards from tokenIndex-1 down to roughStart
        int searchFrom = Math.max(0, tokenIndex - 1);

        for (int i = searchFrom; i >= roughStart; i--) {
            char c = text.charAt(i);
            if (c == '.' || c == '!' || c == '?' || c == '\n') {
                // snippet starts right after punctuation mark
                return i + 1;
            }
        }

        return roughStart;
    }

    private int adjustEndToSentenceBoundary(String text, int roughEnd, int tokenEndIndex) {
        int length = text.length();
        int searchTo = Math.min(length - 1, roughEnd - 1);

        for (int i = tokenEndIndex; i <= searchTo; i++) {
            char c = text.charAt(i);
            if (c == '.' || c == '!' || c == '?' || c == '\n') {
                // snippet ends at this punctuation mark (inclusive)
                return i + 1; // +1 to include punctuation
            }
        }

        // no sentence boundary in range -> keep rough end
        return roughEnd;
    }
}
