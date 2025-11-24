package com.github.stepanterkun.searchengine.document.domain.mapper;

import com.github.stepanterkun.searchengine.document.api.dto.DocumentCreateDto;
import com.github.stepanterkun.searchengine.document.api.dto.DocumentDto;
import com.github.stepanterkun.searchengine.document.domain.model.Document;
import com.github.stepanterkun.searchengine.document.persistence.entity.DocumentEntity;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Mapper between domain documents, persistence entities and API DTOs.
 */
@Component
public class DocumentMapper {

    public DocumentDto toDto(Document domain) {
        if (domain == null) {
            return null;
        }
        return new DocumentDto(
                domain.getId(),
                domain.getTitle(),
                domain.getStatus()
        );
    }

    public List<DocumentDto> toDtoList(List<Document> documents) {
        if (documents == null) {
            return List.of();
        }
        return documents.stream()
                       .map(this::toDto)
                       .toList();
    }

    /**
     * Maps create DTO + ownerId to new domain document.
     * Uses factory method {@link Document#newDocument(String, String, Long)} to set initial status.
     */
    public Document toDomain(DocumentCreateDto createDto, Long ownerId) {
        if (createDto == null) {
            return null;
        }
        // create new domain document with NEW status
        return Document.newDocument(
                createDto.title(),
                createDto.content(),
                ownerId
        );
    }

    public Document toDomain(DocumentEntity entity) {
        if (entity == null) {
            return null;
        }

        return new Document(
                entity.getId(),
                entity.getTitle(),
                entity.getContent(),
                entity.getOwnerId(),
                entity.getStatus()
        );
    }

    public List<Document> toDomainList(List<DocumentEntity> entities) {
        if (entities == null) {
            return List.of();
        }
        return entities.stream()
                       .map(this::toDomain)
                       .toList();
    }

    public DocumentEntity toEntity(Document domain) {
        if (domain == null) {
            return null;
        }
        return new DocumentEntity(
                domain.getId(),
                domain.getTitle(),
                domain.getContent(),
                domain.getOwnerId(),
                domain.getStatus()
        );
    }
}
