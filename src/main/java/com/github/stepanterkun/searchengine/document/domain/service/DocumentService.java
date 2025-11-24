package com.github.stepanterkun.searchengine.document.domain.service;

import com.github.stepanterkun.searchengine.document.api.dto.DocumentCreateDto;
import com.github.stepanterkun.searchengine.document.api.dto.DocumentDto;
import com.github.stepanterkun.searchengine.document.domain.mapper.DocumentMapper;
import com.github.stepanterkun.searchengine.document.domain.model.Document;
import com.github.stepanterkun.searchengine.document.domain.model.DocumentNotFoundException;
import com.github.stepanterkun.searchengine.document.domain.model.DocumentStatus;
import com.github.stepanterkun.searchengine.document.domain.port.DocumentRepository;
import com.github.stepanterkun.searchengine.search.domain.port.SearchIndex;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Application service for working with documents:
 * create, read, delete and keep search index in sync.
 */
@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository repository;
    private final DocumentMapper mapper;
    private final SearchIndex searchIndex;

    public DocumentService(DocumentRepository repository,
                           DocumentMapper mapper,
                           SearchIndex searchIndex) {
        this.repository = repository;
        this.mapper = mapper;
        this.searchIndex = searchIndex;
    }

    /**
     * Creates a new document for the given owner and indexes it for search.
     */
    @Transactional
    public DocumentDto create(Long ownerId,  DocumentCreateDto createDto) {
        Document toSave = mapper.toDomain(createDto, ownerId);
        toSave.setStatus(DocumentStatus.INDEXING);

        Document savedIndexing = repository.save(toSave);

        try {
            // index document content in memory
            searchIndex.index(savedIndexing);
            savedIndexing.setStatus(DocumentStatus.READY);
        } catch (Exception e) {
            log.error(
                    "Failed to index document: id={}, ownerId={}",
                    savedIndexing.getId(),
                    savedIndexing.getOwnerId(),
                    e
            );

            savedIndexing.setStatus(DocumentStatus.FAILED);
            repository.save(savedIndexing);
            throw e;
        }

        Document savedReady = repository.save(savedIndexing);
        return mapper.toDto(savedReady);
    }

    public List<DocumentDto> getAllForOwner(Long ownerId) {
        List<DocumentDto> dtoList = repository.findAllByOwnerId(ownerId)
                                            .stream()
                                            .map(mapper::toDto)
                                            .toList();

        if (dtoList.isEmpty()) {
            throw new DocumentNotFoundException("User does not have any documents loaded");
        }

        return dtoList;
    }


    public DocumentDto getByIdForOwner(Long id, Long ownerId) throws DocumentNotFoundException {
        Document doc = repository.findByIdAndOwnerId(id, ownerId)
                               .orElseThrow(() -> new DocumentNotFoundException(id));

        return mapper.toDto(doc);
    }

    @Transactional
    public void deleteByIdForOwner(Long id, Long ownerId) throws DocumentNotFoundException {
        log.debug("Deleting document: id={}, ownerId={}", id, ownerId);

        Document doc = repository.findByIdAndOwnerId(id, ownerId)
                               .orElseThrow(() -> new DocumentNotFoundException(id));

        // remove from storage
        repository.deleteDocument(doc);
        // remove from in-memory search index
        searchIndex.remove(id);

        log.info("Document deleted: id={}, ownerId={}", id, ownerId);
    }

    @Transactional
    public void deleteAllForOwner(Long ownerId) {
        log.debug("Deleting all documents: ownerId={}", ownerId);

        List<Document> docs = repository.findAllByOwnerId(ownerId);

        if (docs.isEmpty()) {
            throw new DocumentNotFoundException(
                    "Cannot delete documents: User does not have any documents loaded"
            );
        }

        // delete each document and keep search index in sync
        for (Document doc : docs) {
            repository.deleteDocument(doc);
            searchIndex.remove(doc.getId());
        }

        log.info("All documents deleted: ownerId={}", ownerId);
    }
}
