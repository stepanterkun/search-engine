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

import static org.assertj.core.api.Assertions.assertThat;
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
        // given
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

}
