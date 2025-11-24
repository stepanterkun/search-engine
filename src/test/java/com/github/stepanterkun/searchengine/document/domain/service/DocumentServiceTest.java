package com.github.stepanterkun.searchengine.document.domain.service;

import com.github.stepanterkun.searchengine.document.api.dto.DocumentCreateDto;
import com.github.stepanterkun.searchengine.document.api.dto.DocumentDto;
import com.github.stepanterkun.searchengine.document.domain.mapper.DocumentMapper;
import com.github.stepanterkun.searchengine.document.domain.model.Document;
import com.github.stepanterkun.searchengine.document.domain.model.DocumentNotFoundException;
import com.github.stepanterkun.searchengine.document.domain.model.DocumentStatus;
import com.github.stepanterkun.searchengine.document.domain.port.DocumentRepository;
import com.github.stepanterkun.searchengine.search.domain.port.SearchIndex;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository repository;

    @Mock
    private DocumentMapper mapper;

    @Mock
    private SearchIndex searchIndex;

    @InjectMocks
    private DocumentService service;

    @Test
    void create_shouldPassCorrectDocumentToRepository() {
        Long ownerId = 42L;
        DocumentCreateDto createDto = new DocumentCreateDto("Test title", "Test content");

        Document toSave = new Document(
                null,
                createDto.title(),
                createDto.content(),
                ownerId,
                DocumentStatus.NEW
        );

        Document saved = new Document(
                1L,
                "Test title",
                "Test content",
                ownerId,
                DocumentStatus.READY
        );

        DocumentDto expecting = new DocumentDto(saved.getId(), saved.getTitle(), saved.getStatus());

        when(mapper.toDomain(createDto, ownerId)).thenReturn(toSave);
        when(repository.save(any(Document.class))).thenReturn(saved);
        when(mapper.toDto(saved)).thenReturn(expecting);

        DocumentDto result = service.create(ownerId, createDto);

        assertThat(result)
                .isNotNull()
                .extracting(DocumentDto::id, DocumentDto::title, DocumentDto::status)
                .containsExactly(expecting.id(), expecting.title(), expecting.status());

        ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
        verify(repository, atLeastOnce()).save(captor.capture());

        Document firstSaved = captor.getAllValues().get(0);
        assertThat(firstSaved)
                .extracting(
                        Document::getTitle,
                        Document::getContent,
                        Document::getOwnerId
                )
                .containsExactly(
                        "Test title",
                        "Test content",
                        ownerId
                );

        verify(searchIndex).index(any(Document.class));

        verify(mapper).toDomain(createDto, ownerId);
        verify(mapper).toDto(saved);

        verifyNoMoreInteractions(mapper, repository, searchIndex);
    }

    @Test
    void getAllForOwner_whenDocumentsExist_shouldReturnMappedDtos() {
        Long ownerId = 42L;

        Document doc1 = new Document(
                1L,
                "Title 1",
                "Content 1",
                ownerId,
                DocumentStatus.READY
        );

        Document doc2 = new Document(
                2L,
                "Title 2",
                "Content 2",
                ownerId,
                DocumentStatus.NEW
        );

        DocumentDto expectingDto1 = new DocumentDto(doc1.getId(), doc1.getTitle(), doc1.getStatus());
        DocumentDto expectingDto2 = new DocumentDto(doc2.getId(), doc2.getTitle(), doc2.getStatus());

        when(repository.findAllByOwnerId(ownerId)).thenReturn(List.of(doc1, doc2));
        when(mapper.toDto(doc1)).thenReturn(expectingDto1);
        when(mapper.toDto(doc2)).thenReturn(expectingDto2);

        List<DocumentDto> result = service.getAllForOwner(ownerId);

        assertThat(result)
                .isNotNull()
                .hasSize(2);

        assertThat(result.get(0))
                .extracting(DocumentDto::id, DocumentDto::title, DocumentDto::status)
                .containsExactly(expectingDto1.id(), expectingDto1.title(), expectingDto1.status());

        assertThat(result.get(1))
                .extracting(DocumentDto::id, DocumentDto::title, DocumentDto::status)
                .containsExactly(expectingDto2.id(), expectingDto2.title(), expectingDto2.status());

        verify(repository).findAllByOwnerId(ownerId);
        verify(mapper).toDto(doc1);
        verify(mapper).toDto(doc2);
        verifyNoMoreInteractions(mapper, repository);
        verifyNoInteractions(searchIndex);
    }

    @Test
    void getAllForOwner_whenNoDocuments_shouldThrowDocumentNotFoundException() {
        Long ownerId = 42L;

        when(repository.findAllByOwnerId(ownerId)).thenReturn(List.of());

        DocumentNotFoundException ex = assertThrows(
                DocumentNotFoundException.class,
                () -> service.getAllForOwner(ownerId)
        );

        assertThat(ex.getMessage().toLowerCase())
                .contains("user does not have any documents loaded");

        verify(repository).findAllByOwnerId(ownerId);
        verifyNoInteractions(mapper, searchIndex);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void getByIdForOwner_whenDocumentExists_shouldReturnMappedDto() {
        Long id = 1L;
        Long ownerId = 42L;

        Document doc = new Document(
                id,
                "Title 1",
                "Content 1",
                ownerId,
                DocumentStatus.READY
        );

        DocumentDto expecting = new DocumentDto(id, doc.getTitle(), doc.getStatus());

        when(repository.findByIdAndOwnerId(id, ownerId)).thenReturn(Optional.of(doc));
        when(mapper.toDto(doc)).thenReturn(expecting);

        DocumentDto result = service.getByIdForOwner(id, ownerId);

        assertThat(result)
                .isNotNull()
                .extracting(DocumentDto::id, DocumentDto::title, DocumentDto::status)
                .containsExactly(expecting.id(), expecting.title(), expecting.status());

        verify(repository).findByIdAndOwnerId(id, ownerId);
        verify(mapper).toDto(doc);
        verifyNoMoreInteractions(mapper, repository);
        verifyNoInteractions(searchIndex);
    }

    @Test
    void getByIdForOwner_whenDocumentNotFound_shouldThrowDocumentNotFoundException() {
        Long id = 1L;
        Long ownerId = 42L;

        when(repository.findByIdAndOwnerId(id, ownerId)).thenReturn(Optional.empty());

        DocumentNotFoundException ex = assertThrows(
                DocumentNotFoundException.class,
                () -> service.getByIdForOwner(id, ownerId)
        );

        assertTrue(ex.getMessage().contains("Document not found with id=" + id)
                           || ex.getMessage().contains(String.valueOf(id)));

        verify(repository).findByIdAndOwnerId(id, ownerId);
        verifyNoMoreInteractions(repository);
        verifyNoInteractions(mapper, searchIndex);
    }
}
