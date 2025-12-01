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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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

    private List<DocumentSummary> generateSummaries(int amount) {
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

        List<DocumentSummary> all = generateSummaries(12); // ids 1..12

        int pageNumber = 2; // 1-based
        int pageSize = 5;

        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize); // page=1 (0-based)

        // content for 2nd page: id 6..10
        List<DocumentSummary> pageContent = all.subList(5, 10);
        Page<DocumentSummary> page = new PageImpl<>(pageContent, pageable, all.size());

        when(searchIndex.search(ownerId, query, pageable)).thenReturn(page);

        SearchResultDto result = service.searchAllDocumentsByQuery(ownerId, query, pageNumber, pageSize);

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
                        pageNumber,
                        pageSize,
                        12L,
                        3,
                        true, // hasPrevious
                        true  // hasNext
                );

        assertThat(result.documentSummaries()).hasSize(5);
        assertThat(result.documentSummaries().get(0).documentId())
                .isEqualTo(all.get(5).documentId()); // id=6
        assertThat(result.documentSummaries().get(4).documentId())
                .isEqualTo(all.get(9).documentId()); // id=10

        verify(searchIndex).search(ownerId, query, pageable);
        verifyNoMoreInteractions(searchIndex);
    }

    @Test
    void searchAllDocumentsByQuery_shouldReturnClappedPage() {
        Long ownerId = 42L;
        String query = "java";

        List<DocumentSummary> all = generateSummaries(12);

        int wrongPage = 50;
        int size = 5;

        // первый запрос: очень большая страница, контент пустой, но totalElements известен
        Pageable wrongPageable = PageRequest.of(wrongPage - 1, size); // 49
        Page<DocumentSummary> emptyPage = new PageImpl<>(
                List.of(),
                wrongPageable,
                all.size() // 12 документов всего
        );

        // второй запрос: последняя страница (3-я, 0-based = 2), документы 11 и 12
        Pageable lastPageable = PageRequest.of(2, size);
        List<DocumentSummary> lastPageContent = all.subList(10, 12);
        Page<DocumentSummary> lastPage = new PageImpl<>(
                lastPageContent,
                lastPageable,
                all.size()
        );

        when(searchIndex.search(ownerId, query, wrongPageable)).thenReturn(emptyPage);
        when(searchIndex.search(ownerId, query, lastPageable)).thenReturn(lastPage);

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
                        3,      // перелистнули на последнюю страницу
                        size,
                        12L,
                        3,
                        true,   // hasPrevious
                        false   // hasNext
                );

        assertThat(result.documentSummaries()).hasSize(2);
        assertThat(result.documentSummaries().get(0).documentId())
                .isEqualTo(all.get(10).documentId()); // id=11
        assertThat(result.documentSummaries().get(1).documentId())
                .isEqualTo(all.get(11).documentId()); // id=12

        verify(searchIndex).search(ownerId, query, wrongPageable);
        verify(searchIndex).search(ownerId, query, lastPageable);
        verifyNoMoreInteractions(searchIndex);
    }

    @Test
    void searchAllDocumentsByQuery_whenWrongPageAndSize_shouldReturnDefaultValues() {
        Long ownerId = 42L;
        String query = "java";

        List<DocumentSummary> all = generateSummaries(3);
        Pageable pageable = PageRequest.of(0, 20);
        Page<DocumentSummary> page = new PageImpl<>(all, pageable, all.size());

        when(searchIndex.search(ownerId, query, pageable)).thenReturn(page);

        Integer wrongPage = 0;
        Integer wrongSize = -10;

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
                        1,      // totalPages = 1
                        false,
                        false
                );

        assertThat(result.documentSummaries().size()).isEqualTo(3);
        assertThat(result.documentSummaries().get(0).documentId())
                .isEqualTo(summary(1L).documentId());
        assertThat(result.documentSummaries().get(2).documentId())
                .isEqualTo(summary(3L).documentId());

        verify(searchIndex).search(ownerId, query, pageable);
        verifyNoMoreInteractions(searchIndex);
    }


    @Test
    void searchAllDocumentsByQuery_whenEmptyElements_shouldReturnEmptyList() {
        Long ownerId = 42L;
        String query = "java";

        int defaultPage = 1;
        int defaultSize = 20;

        Pageable pageable = PageRequest.of(defaultPage - 1, defaultSize); // 0,20
        Page<DocumentSummary> emptyPage = Page.empty(pageable);

        when(searchIndex.search(ownerId, query, pageable)).thenReturn(emptyPage);

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
                        false,
                        false
                );

        assertThat(result.documentSummaries()).isEmpty();

        verify(searchIndex).search(ownerId, query, pageable);
        verifyNoMoreInteractions(searchIndex);
    }
}
