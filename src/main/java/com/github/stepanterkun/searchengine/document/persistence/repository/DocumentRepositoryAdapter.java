package com.github.stepanterkun.searchengine.document.persistence.repository;

import com.github.stepanterkun.searchengine.document.domain.mapper.DocumentMapper;
import com.github.stepanterkun.searchengine.document.domain.model.Document;
import com.github.stepanterkun.searchengine.document.domain.model.DocumentNotFoundException;
import com.github.stepanterkun.searchengine.document.domain.port.DocumentRepository;
import com.github.stepanterkun.searchengine.document.persistence.entity.DocumentEntity;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Adapter that connects domain {@link DocumentRepository} with JPA repository.
 */
@Repository
public class DocumentRepositoryAdapter implements DocumentRepository {

    private final DocumentJpaRepository jpaRepository;
    private final DocumentMapper mapper;

    public DocumentRepositoryAdapter(DocumentJpaRepository jpaRepository,
                                     DocumentMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Document save(Document document) {
        DocumentEntity entity = mapper.toEntity(document);
        DocumentEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Document> findByIdAndOwnerId(Long id, Long ownerId) throws DocumentNotFoundException {
        // map JPA entity to domain document
        return jpaRepository.findByIdAndOwnerId(id, ownerId)
                       .map(mapper::toDomain);
    }

    @Override
    public List<Document> findAllByOwnerId(Long ownerId) {
        List<DocumentEntity> entities = jpaRepository.findAllByOwnerId(ownerId);
        return mapper.toDomainList(entities);
    }

    @Override
    public void deleteDocument(Document doc) {
        if (doc == null || doc.getId() == null) {
            return;
        }

        try {
            jpaRepository.deleteByIdAndOwnerId(doc.getId(), doc.getOwnerId());
        } catch (EmptyResultDataAccessException ignored) {
            // nothing to delete
        }
    }

    @Override
    public List<Document> findAll() {
        return mapper.toDomainList(jpaRepository.findAll());
    }
}