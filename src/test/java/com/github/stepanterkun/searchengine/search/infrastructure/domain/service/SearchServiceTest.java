package com.github.stepanterkun.searchengine.search.infrastructure.domain.service;

import com.github.stepanterkun.searchengine.document.domain.model.DocumentStatus;
import com.github.stepanterkun.searchengine.search.api.dto.SearchResultDto;
import com.github.stepanterkun.searchengine.search.domain.model.DocumentSummary;
import com.github.stepanterkun.searchengine.search.domain.port.SearchIndex;
import com.github.stepanterkun.searchengine.search.domain.service.SearchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SearchServiceTest {

    @Mock
    SearchIndex searchIndex;

    @InjectMocks
    SearchService service;

    private List<DocumentSummary> summariesList(int amount) {
        List<DocumentSummary> summaries = new ArrayList<>(amount);

        for (long id = 1; id <= amount; id++) {
            summaries.add(summary(id));
        }

        return summaries;
    }

    private DocumentSummary summary(Long id) {
        return new DocumentSummary(
                id,
                "Title - " + id,
                DocumentStatus.READY,
                ThreadLocalRandom.current().nextDouble(0, 10),
                List.of()
        );
    }

    @Test
    void searchAllDocumentsByQuery_shouldReturnCorrectPageMetadataAndContent() {
        Long ownerId = 42L;
        String query = "java";

        List<DocumentSummary> all = summariesList(12);

        when(searchIndex.search(ownerId, query)).thenReturn(all);

        int page = 2;
        int size = 5;

        SearchResultDto result = service.searchAllDocumentsByQuery(ownerId, query, page, size);

        assertThat(result)
                .isNotNull()
                .extracting(
                        SearchResultDto::originalQuery,
                        SearchResultDto::page,
                        SearchResultDto::size,
                        SearchResultDto::totalElements,
                        SearchResultDto::totalPages,
                        SearchResultDto::hasPrevious,
                        SearchResultDto::hasNext
                ).containsExactly(
                        query,
                        page,
                        size,
                        12L,
                        3,
                        true, // hasPrevious
                        true  // hasNext
                );

        assertThat(result.documentSummaries().size()).isEqualTo(5);
        assertThat(result.documentSummaries().get(0).documentId()).isEqualTo(summary(6L).documentId());
        assertThat(result.documentSummaries().get(4).documentId()).isEqualTo(summary(10L).documentId());

        verify(searchIndex).search(ownerId, query);
        verifyNoMoreInteractions(searchIndex);
    }

    @Test
    void searchAllDocumentsByQuery_shouldReturnClappedPage() {
        Long ownerId = 42L;
        String query = "java";

        List<DocumentSummary> all = summariesList(12);

        when(searchIndex.search(ownerId, query)).thenReturn(all);

        int wrongPage = 50;
        int size = 5;

        SearchResultDto result = service.searchAllDocumentsByQuery(ownerId, query, wrongPage, size);

        assertThat(result)
                .isNotNull()
                .extracting(
                        SearchResultDto::originalQuery,
                        SearchResultDto::page,
                        SearchResultDto::size,
                        SearchResultDto::totalElements,
                        SearchResultDto::totalPages,
                        SearchResultDto::hasPrevious,
                        SearchResultDto::hasNext
                ).containsExactly(
                        query,
                        3,
                        size,
                        12L,
                        3,
                        true,   // hasPrevious
                        false   // hasNext
                );

        // last page should contain only 11 and 12 docs
        assertThat(result.documentSummaries().size()).isEqualTo(2);
        assertThat(result.documentSummaries().get(0).documentId())
                .isEqualTo(summary(11L).documentId());
        assertThat(result.documentSummaries().get(1).documentId())
                .isEqualTo(summary(12L).documentId());

        verify(searchIndex).search(ownerId, query);
        verifyNoMoreInteractions(searchIndex);
    }

    @Test
    void searchAllDocumentsByQuery_whenWrongPageAndSize_shouldReturnDefaultValues() {
        Long ownerId = 42L;
        String query = "java";

        List<DocumentSummary> all = summariesList(3); // all docs on one default page

        when(searchIndex.search(ownerId, query)).thenReturn(all);

        Integer wrongPage = 0;    // должен стать PAGE_DEFAULT = 1
        Integer wrongSize = -10;  // должен стать SIZE_DEFAULT = 20

        SearchResultDto result = service.searchAllDocumentsByQuery(ownerId, query, wrongPage, wrongSize);

        assertThat(result)
                .isNotNull()
                .extracting(
                        SearchResultDto::originalQuery,
                        SearchResultDto::page,
                        SearchResultDto::size,
                        SearchResultDto::totalElements,
                        SearchResultDto::totalPages,
                        SearchResultDto::hasPrevious,
                        SearchResultDto::hasNext
                ).containsExactly(
                        query,
                        1,      // PAGE_DEFAULT
                        20,     // SIZE_DEFAULT
                        3L,
                        1,
                        false,
                        false
                );

        assertThat(result.documentSummaries().size()).isEqualTo(3);
        assertThat(result.documentSummaries().get(0).documentId())
                .isEqualTo(summary(1L).documentId());
        assertThat(result.documentSummaries().get(2).documentId())
                .isEqualTo(summary(3L).documentId());

        verify(searchIndex).search(ownerId, query);
        verifyNoMoreInteractions(searchIndex);
    }

    @Test
    void searchAllDocumentsByQuery_whenEmptyElements_shouldReturnEmptyList() {
        Long ownerId = 42L;
        String query = "java";

        when(searchIndex.search(ownerId, query)).thenReturn(List.of());

        // page and size = null => implement defaults (1 and 20)
        SearchResultDto result = service.searchAllDocumentsByQuery(ownerId, query, null, null);

        assertThat(result)
                .isNotNull()
                .extracting(
                        SearchResultDto::originalQuery,
                        SearchResultDto::page,
                        SearchResultDto::size,
                        SearchResultDto::totalElements,
                        SearchResultDto::totalPages,
                        SearchResultDto::hasPrevious,
                        SearchResultDto::hasNext
                ).containsExactly(
                        query,
                        1,
                        20,
                        0L,
                        0,
                        false, // hasPrevious
                        false  // hasNext
                );

        assertThat(result.documentSummaries()).isEmpty();

        verify(searchIndex).search(ownerId, query);
        verifyNoMoreInteractions(searchIndex);
    }
}
