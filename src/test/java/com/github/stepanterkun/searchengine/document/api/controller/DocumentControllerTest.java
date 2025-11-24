package com.github.stepanterkun.searchengine.document.api.controller;

import com.github.stepanterkun.searchengine.document.api.dto.DocumentCreateDto;
import com.github.stepanterkun.searchengine.document.api.dto.DocumentDto;
import com.github.stepanterkun.searchengine.document.domain.model.DocumentNotFoundException;
import com.github.stepanterkun.searchengine.document.domain.model.DocumentStatus;
import com.github.stepanterkun.searchengine.document.domain.service.DocumentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DocumentController}.
 * controller is tested with mocked {@link DocumentService}
 */
@ExtendWith(MockitoExtension.class)
class DocumentControllerTest {

    @Mock
    private DocumentService service;

    @InjectMocks
    private DocumentController controller;

    @Test
    void getAllForOwner_whenDocumentsExist_shouldReturnOkWithBody() {
        Long ownerId = 42L;

        DocumentDto dto1 = new DocumentDto(1L, "Title 1", DocumentStatus.READY);
        DocumentDto dto2 = new DocumentDto(2L, "Title 2", DocumentStatus.NEW);
        List<DocumentDto> expected = List.of(dto1, dto2);

        when(service.getAllForOwner(ownerId)).thenReturn(expected);

        ResponseEntity<List<DocumentDto>> response = controller.getAllForOwner(ownerId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .isNotNull()
                .containsExactlyElementsOf(expected);

        verify(service).getAllForOwner(ownerId);
        verifyNoMoreInteractions(service);
    }

    @Test
    void getAllForOwner_whenServiceThrowsNotFound_shouldPropagateException() {
        Long ownerId = 52L;

        when(service.getAllForOwner(ownerId))
                .thenThrow(new DocumentNotFoundException("User does not have any documents loaded"));

        DocumentNotFoundException ex = assertThrows(
                DocumentNotFoundException.class,
                () -> controller.getAllForOwner(ownerId)
        );

        assertThat(ex)
                .isInstanceOf(DocumentNotFoundException.class)
                .hasMessageContaining("User does not have any documents loaded");

        verify(service).getAllForOwner(ownerId);
        verifyNoMoreInteractions(service);
    }

    @Test
    void getByIdAndOwnerId_shouldReturnOkWithBody() {
        Long id = 1L;
        Long ownerId = 42L;

        DocumentDto expected = new DocumentDto(id, "Title", DocumentStatus.NEW);

        when(service.getByIdForOwner(id, ownerId)).thenReturn(expected);

        ResponseEntity<DocumentDto> response = controller.getByIdForOwner(id, ownerId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        assertThat(response.getBody())
                .isNotNull()
                .extracting(DocumentDto::id, DocumentDto::title, DocumentDto::status)
                .containsExactly(id, "Title", DocumentStatus.NEW);

        verify(service).getByIdForOwner(id, ownerId);
        verifyNoMoreInteractions(service);
    }

    @Test
    void getByIdForOwner_whenServiceThrowsNotFound_shouldPropagateException() {
        Long id = 1L;
        Long ownerId = 42L;

        when(service.getByIdForOwner(id, ownerId))
                .thenThrow(new DocumentNotFoundException(id));

        DocumentNotFoundException ex = assertThrows(
                DocumentNotFoundException.class,
                () -> controller.getByIdForOwner(id, ownerId)
        );

        assertThat(ex)
                .hasMessageContaining(String.valueOf(id));

        verify(service).getByIdForOwner(id, ownerId);
        verifyNoMoreInteractions(service);
    }

    @Test
    void create_shouldReturnCreatedAndBody() {
        Long ownerId = 42L;

        DocumentCreateDto createDto = new DocumentCreateDto("Title", "Content");
        DocumentDto expected = new DocumentDto(10L, "Title", DocumentStatus.NEW);

        when(service.create(ownerId, createDto)).thenReturn(expected);

        ResponseEntity<DocumentDto> response = controller.create(ownerId, createDto);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody())
                .isNotNull()
                .extracting(DocumentDto::id, DocumentDto::title, DocumentDto::status)
                .containsExactly(expected.id(), expected.title(), expected.status());

        // use captors to verify that controller passes correct arguments to service
        ArgumentCaptor<Long> ownerIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<DocumentCreateDto> dtoCaptor = ArgumentCaptor.forClass(DocumentCreateDto.class);

        verify(service).create(ownerIdCaptor.capture(), dtoCaptor.capture());

        assertThat(ownerIdCaptor.getValue())
                .isNotNull()
                .isEqualTo(ownerId);

        assertThat(dtoCaptor.getValue())
                .isNotNull()
                .extracting(DocumentCreateDto::title, DocumentCreateDto::content)
                .containsExactly("Title", "Content");

        verifyNoMoreInteractions(service);
    }
}