package com.github.stepanterkun.searchengine.document.domain.mapper;

import com.github.stepanterkun.searchengine.document.api.dto.DocumentCreateDto;
import com.github.stepanterkun.searchengine.document.api.dto.DocumentDto;
import com.github.stepanterkun.searchengine.document.domain.model.Document;
import com.github.stepanterkun.searchengine.document.domain.model.DocumentStatus;
import com.github.stepanterkun.searchengine.document.persistence.entity.DocumentEntity; // поправь пакет, если нужно
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class DocumentMapperTest {

    private final DocumentMapper mapper = new DocumentMapper();

    @Test
    void toDto_shouldMapAllFieldsFromDomain() {
        Long id = 1L;
        Long ownerId = 42L;

        Document domain = new Document(
                id,
                "Some title",
                "Some contents",
                ownerId,
                DocumentStatus.READY
        );

        DocumentDto result = mapper.toDto(domain);

        assertThat(result)
                .isNotNull()
                .extracting(DocumentDto::id, DocumentDto::title, DocumentDto::status)
                .containsExactly(domain.getId(), domain.getTitle(), domain.getStatus());
    }

    @Test
    void toDomain_shouldMapAllFieldsFromCreateDto() {
        Long ownerId = 52L;
        DocumentCreateDto createDto = new DocumentCreateDto(
                "title",
                "content"
        );

        Document result = mapper.toDomain(createDto, ownerId);

        assertNotNull(result);
        // новый документ – id ещё нет (если у тебя так заведено)
        assertNull(result.getId());
        assertEquals(createDto.title(),   result.getTitle());
        assertEquals(createDto.content(), result.getContent());
        assertEquals(ownerId,             result.getOwnerId());
        assertEquals(DocumentStatus.NEW,  result.getStatus());
    }

    @Test
    void toDomain_shouldMapAllFieldsFromEntity() {
        Long ownerId = 42L;

        DocumentEntity entity = new DocumentEntity(
                1L,
                "a title",
                "content",
                ownerId,
                DocumentStatus.FAILED
        );

        Document result = mapper.toDomain(entity);

        assertNotNull(result);
        assertEquals(entity.getId(),      result.getId());
        assertEquals(entity.getTitle(),   result.getTitle());
        assertEquals(entity.getContent(), result.getContent());
        assertEquals(entity.getOwnerId(), result.getOwnerId());
        assertEquals(entity.getStatus(),  result.getStatus());
    }

    @Test
    void toEntity_shouldMapAllFieldsFromDomain() {
        Long ownerId = 42L;

        Document domain = new Document(
                1L,
                "title",
                "content",
                ownerId,
                DocumentStatus.FAILED
        );

        DocumentEntity result = mapper.toEntity(domain);

        assertNotNull(result);
        assertEquals(domain.getId(),      result.getId());
        assertEquals(domain.getTitle(),   result.getTitle());
        assertEquals(domain.getContent(), result.getContent());
        assertEquals(domain.getOwnerId(), result.getOwnerId());
        assertEquals(domain.getStatus(),  result.getStatus());
    }

    @Test
    void toDtoList_shouldMapAllElementsAndPreserveOrder() {
        List<Document> domains = List.of(
                new Document(1L, "Title 1", "Content 1", 42L, DocumentStatus.NEW),
                new Document(2L, "Title 2", "Content 2", 52L, DocumentStatus.READY)
        );

        List<DocumentDto> result = mapper.toDtoList(domains);

        assertNotNull(result);
        assertEquals(domains.size(), result.size());

        for (int i = 0; i < domains.size(); i++) {
            Document domain = domains.get(i);
            DocumentDto dto = result.get(i);

            assertEquals(domain.getId(),     dto.id());
            assertEquals(domain.getTitle(),  dto.title());
            assertEquals(domain.getStatus(), dto.status());
        }
    }

    @Test
    void toDomainList_shouldMapAllElementsAndPreserveOrder() {
        Long ownerId = 42L;

        List<DocumentEntity> entities = List.of(
                new DocumentEntity(1L, "title 1", "content 1", ownerId, DocumentStatus.NEW),
                new DocumentEntity(2L, "title 2", "content 2", ownerId, DocumentStatus.READY)
        );

        List<Document> result = mapper.toDomainList(entities);

        assertNotNull(result);
        assertEquals(entities.size(), result.size());

        for (int i = 0; i < entities.size(); i++) {
            DocumentEntity entity = entities.get(i);
            Document domain = result.get(i);

            assertEquals(entity.getId(),      domain.getId());
            assertEquals(entity.getTitle(),   domain.getTitle());
            assertEquals(entity.getContent(), domain.getContent());
            assertEquals(entity.getOwnerId(), domain.getOwnerId());
            assertEquals(entity.getStatus(),  domain.getStatus());
        }
    }

    @Test
    void toDtoList_shouldReturnEmptyList_whenInputEmpty() {
        List<DocumentDto> result = mapper.toDtoList(List.of());

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void toDomainList_shouldReturnEmptyList_whenInputEmpty() {
        List<Document> result = mapper.toDomainList(List.of());

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
