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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
public class DocumentServiceSpringBootTest {

    @Mock
    private DocumentRepository repository;

    @Mock
    private DocumentMapper mapper;

    @Mock
    private SearchIndex searchIndex;

    @InjectMocks
    private DocumentService service;

    @Test
    void create_shouldMapSaveAndReturnDto() {
        Long ownerId = 1L;
        DocumentCreateDto createDto = new DocumentCreateDto("Title 1", "Content 1");

        // domain document that mapper returns
        Document toSave = Document.newDocument(
                createDto.title(),
                createDto.content(),
                ownerId
        );

        // document returned by repository (e.g. with generated id)
        Document saved = new Document(
                1L,
                toSave.getTitle(),
                toSave.getContent(),
                toSave.getOwnerId(),
                toSave.getStatus()
        );

        DocumentDto expectedDto = new DocumentDto(saved.getId(), saved.getTitle(), saved.getStatus());

        when(mapper.toDomain(createDto, ownerId)).thenReturn(toSave);
        when(repository.save(any(Document.class))).thenReturn(saved);
        when(mapper.toDto(saved)).thenReturn(expectedDto);

        // act
        DocumentDto result = service.create(ownerId, createDto);

        // assert result dto
        assertThat(result)
                .isNotNull()
                .isEqualTo(expectedDto);

        // verify mapper interactions
        verify(mapper).toDomain(createDto, ownerId);
        verify(mapper).toDto(saved);
        verifyNoMoreInteractions(mapper);

        // capture first document passed to repository and check its basic fields
        ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
        verify(repository, atLeastOnce()).save(captor.capture());

        Document firstSaved = captor.getAllValues().get(0);
        assertThat(firstSaved.getTitle()).isEqualTo("Title 1");
        assertThat(firstSaved.getContent()).isEqualTo("Content 1");
        assertThat(firstSaved.getOwnerId()).isEqualTo(ownerId);

        // we do not verify repository interactions further
        // to keep the test stable if internal save logic changes
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

        DocumentDto expecting = new DocumentDto(doc.getId(), doc.getTitle(), doc.getStatus());

        when(repository.findByIdAndOwnerId(id, ownerId)).thenReturn(Optional.of(doc));
        when(mapper.toDto(doc)).thenReturn(expecting);

        DocumentDto result = service.getByIdForOwner(id, ownerId);

        assertThat(result)
                .extracting(DocumentDto::id,DocumentDto::title,DocumentDto::status)
                .containsExactly(id, "Title 1", DocumentStatus.READY);

        verify(repository).findByIdAndOwnerId(id, ownerId);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void getByIdForOwner_whenDocumentNotFound_shouldThrowDocumentNotFoundException() {
        Long id = 1L;
        Long ownerId = 42L;

        when(repository.findByIdAndOwnerId(id, ownerId))
                .thenReturn(Optional.empty());

        DocumentNotFoundException ex = assertThrows(
                DocumentNotFoundException.class,
                () -> service.getByIdForOwner(id, ownerId)
        );

        assertTrue(ex.getMessage().contains("Document not found with id=" + id)
                           || ex.getMessage().contains(String.valueOf(id)));

        verify(repository, times(1)).findByIdAndOwnerId(id, ownerId);
        verifyNoMoreInteractions(repository);
        verifyNoInteractions(mapper);
    }

    @Test
    void getAllForOwner_whenDocumentsExist_shouldReturnMappedDtos() {
        Long ownerId = 42L;

        Document doc1 = new Document(1L, "Title 1", "Content 1", ownerId, DocumentStatus.READY);
        Document doc2 = new Document(2L, "Title 2", "Content 2", ownerId, DocumentStatus.NEW);

        DocumentDto dto1 = new DocumentDto(doc1.getId(), doc1.getTitle(), doc1.getStatus());
        DocumentDto dto2 = new DocumentDto(doc2.getId(), doc2.getTitle(), doc2.getStatus());

        when(repository.findAllByOwnerId(ownerId)).thenReturn(List.of(doc1, doc2));
        when(mapper.toDto(doc1)).thenReturn(dto1);
        when(mapper.toDto(doc2)).thenReturn(dto2);


        List<DocumentDto> result = service.getAllForOwner(ownerId);

        assertThat(result)
                .hasSize(2)
                .extracting(DocumentDto::id, DocumentDto::title, DocumentDto::status)
                .containsExactly(
                        tuple(doc1.getId(), doc1.getTitle(), doc1.getStatus()),
                        tuple(doc2.getId(), doc2.getTitle(), doc2.getStatus())
                );

        verify(repository).findAllByOwnerId(ownerId);
        verify(mapper).toDto(doc1);
        verify(mapper).toDto(doc2);
        verifyNoMoreInteractions(mapper, repository);
    }

    @Test
    void getAllForOwner_whenNoDocuments_shouldThrowDocumentNotFoundException() {
        Long ownerId = 42L;

        when(repository.findAllByOwnerId(ownerId)).thenReturn(List.of());

        DocumentNotFoundException ex = assertThrows(
                DocumentNotFoundException.class,
                () -> service.getAllForOwner(ownerId)
        );

        // check that message explains that user has no documents
        assertTrue(ex.getMessage().contains("User does not have any documents loaded"));

        verify(repository).findAllByOwnerId(ownerId);
        verifyNoMoreInteractions(repository);
    }

}
