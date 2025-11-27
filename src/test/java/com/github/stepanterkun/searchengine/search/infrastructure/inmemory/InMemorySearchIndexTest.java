package com.github.stepanterkun.searchengine.search.infrastructure.inmemory;

import com.github.stepanterkun.searchengine.document.domain.model.Document;
import com.github.stepanterkun.searchengine.document.domain.model.DocumentStatus;
import com.github.stepanterkun.searchengine.document.domain.port.DocumentRepository;
import com.github.stepanterkun.searchengine.search.domain.model.DocumentSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link InMemorySearchIndex}.
 * Uses pure Mockito, without Spring context.
 */
@ExtendWith(MockitoExtension.class)
class InMemorySearchIndexTest {

    @Mock
    private DocumentRepository repository;

    @InjectMocks
    private InMemorySearchIndex searchIndex;

    @Test
    void index_shouldIndexDocumentWithoutChangingStatus() {
        Document doc = new Document(
                1L,
                "Title",
                "Some content to index",
                42L,
                DocumentStatus.NEW
        );

        searchIndex.index(doc);

        // index should not touch domain status
        assertThat(doc.getStatus()).isEqualTo(DocumentStatus.NEW);
    }

    @Test
    void search_shouldReturnDocumentSummaryForOwner() {
        Long ownerId = 42L;
        Long docId = 1L;
        String content = "Java search engine test content";

        Document doc = new Document(
                docId,
                "Title",
                content,
                ownerId,
                DocumentStatus.READY
        );

        searchIndex.index(doc);

        when(repository.findByIdAndOwnerId(docId, ownerId)).thenReturn(Optional.of(doc));

        List<DocumentSummary> result = searchIndex.search(ownerId, "search engine");

        assertThat(result)
                .hasSize(1);

        DocumentSummary summary = result.get(0);

        assertThat(summary.documentId()).isEqualTo(docId);
        assertThat(summary.documentTitle()).isEqualTo("Title");
        assertThat(summary.documentStatus()).isEqualTo(DocumentStatus.READY);
        assertThat(summary.relevanceScore()).isGreaterThan(0);

        assertThat(summary.wordSnippets())
                .isNotNull()
                .hasSize(2);

        verify(repository).findByIdAndOwnerId(docId, ownerId);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void search_whenNoDocs_shouldReturnEmptyList() {
        Long ownerId = 123L;
        Long doc1Id = 42L;
        Long doc2Id = 52L;

        Document doc1 = new Document(
                doc1Id,
                "Test title 1",
                "Test containing word java 1. Spring",
                ownerId,
                DocumentStatus.READY
        );

        Document doc2 = new Document(
                doc2Id,
                "Test title 2",
                "Test containing word java 2",
                ownerId,
                DocumentStatus.READY
        );

        searchIndex.index(doc1);
        searchIndex.index(doc2);

        when(repository.findByIdAndOwnerId(doc1Id, ownerId)).thenReturn(Optional.of(doc1));
        // not needed, because this doc was sorted before calling repository
        // when(repository.findByIdAndOwnerId(doc2Id, ownerId)).thenReturn(Optional.of(doc2));

        List<DocumentSummary> result = searchIndex.search(ownerId, "Java spring");

        assertThat(result)
                .isNotNull()
                .hasSize(1);

        assertThat(result.get(0).relevanceScore())
                .isEqualTo(Math.log(3.0 / 2.0)); // smoothed IDF: log((N + 1) / (df + 1))


    }

    @Test
    void search_shouldUseIdfAndMinScoreToFilterLessRelevantDocs() {
        Long ownerId = 42L;
        Long doc1Id = 42L;
        Long doc2Id = 52L;

        Document doc1 = new Document(
                doc1Id,
                "Test title 1",
                "Test containing word java 1. Spring",
                ownerId,
                DocumentStatus.READY
        );

        Document doc2 = new Document(
                doc2Id,
                "Test title 2",
                "Test containing word java 2",
                ownerId,
                DocumentStatus.READY
        );

        searchIndex.index(doc1);
        searchIndex.index(doc2);

        when(repository.findByIdAndOwnerId(doc1Id, ownerId)).thenReturn(Optional.of(doc1));

        // when search query is "Java spring":
        // - term "java" gives idf = 0 (contains in both docs)
        // - term "spring" contains only in doc1 → idf = log((N + 1) / (df + 1)) = log(3 / 2)
        List<DocumentSummary> result = searchIndex.search(ownerId, "Java spring");

        assertThat(result)
                .isNotNull()
                .hasSize(1);

        DocumentSummary summary = result.get(0);

        assertThat(summary.documentId()).isEqualTo(doc1Id);
        assertThat(summary.documentTitle()).isEqualTo("Test title 1");
        assertThat(summary.documentStatus()).isEqualTo(DocumentStatus.READY);

        double expectedIdfSpring = Math.log(3.0 / 2.0); // smoothed IDF: log((N + 1) / (df + 1))

        assertThat(summary.relevanceScore())
                .isCloseTo(expectedIdfSpring, within(1e-9));

        verify(repository).findByIdAndOwnerId(doc1Id, ownerId);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void search_whenNoDocsForOwner_shouldReturnEmptyList() {
        Long ownerId = 123L;

        List<DocumentSummary> result = searchIndex.search(ownerId, "java");

        assertThat(result).isEmpty();
        verifyNoInteractions(repository);
    }
}
