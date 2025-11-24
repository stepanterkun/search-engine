package com.github.stepanterkun.searchengine.persistence.repository;

import com.github.stepanterkun.searchengine.document.domain.mapper.DocumentMapper;
import com.github.stepanterkun.searchengine.document.domain.model.Document;
import com.github.stepanterkun.searchengine.document.domain.model.DocumentStatus;
import com.github.stepanterkun.searchengine.document.domain.port.DocumentRepository;
import com.github.stepanterkun.searchengine.document.persistence.entity.DocumentEntity;
import com.github.stepanterkun.searchengine.document.persistence.repository.DocumentRepositoryAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import({DocumentRepositoryAdapter.class, DocumentMapper.class, DocumentEntity.class})
class DocumentRepositoryAdapterTest {

    @Autowired
    private DocumentRepository repository;  // domain port, implemented by adapter

    @Autowired
    private TestEntityManager em;   // used to prepare test data directly in db

    @Test
    void save_shouldSaveToDbAndReturnWithId() {
        Long ownerId = 100L;
        Document toSave = Document.newDocument("Title", "Content", ownerId);

        Document saved = repository.save(toSave);

        assertThat(saved).isNotNull();
        assertThat(saved.getId()).isNotNull();

        assertThat(saved)
                .extracting(
                        Document::getTitle,
                        Document::getContent,
                        Document::getOwnerId,
                        Document::getStatus
                )
                .containsExactly(
                        "Title",
                        "Content",
                        ownerId,
                        DocumentStatus.NEW
                );
    }

    @Test
    void findAllByOwnerId_shouldReturnOnlyDocumentsOfGivenOwner() {
        Long owner1 = 1L;
        Long owner2 = 2L;

        DocumentEntity doc1Owner1 = new DocumentEntity(
                null,
                "Title 1",
                "Content 1",
                owner1,
                DocumentStatus.NEW
        );
        DocumentEntity doc2Owner1 = new DocumentEntity(
                null,
                "Title 2",
                "Content 2",
                owner1,
                DocumentStatus.READY
        );
        DocumentEntity docOwner2 = new DocumentEntity(
                null,
                "Other",
                "Other content",
                owner2,
                DocumentStatus.NEW
        );

        em.persist(doc1Owner1);
        em.persist(doc2Owner1);
        em.persist(docOwner2);
        em.flush();

        List<Document> result = repository.findAllByOwnerId(owner1);

        assertThat(result)
                .hasSize(2)
                .extracting(Document::getOwnerId)
                .containsOnly(owner1);

        assertThat(result)
                .extracting(Document::getTitle)
                .containsExactlyInAnyOrder("Title 1", "Title 2");
    }

    @Test
    void findAllByOwnerId_whenNoDocuments_shouldReturnEmptyList() {
        Long ownerId = 1L;

        List<Document> docs = repository.findAllByOwnerId(ownerId);

        assertThat(docs).isEmpty();
    }

    @Test
    void findByIdAndOwnerId_shouldReturnDocumentById() {
        Long ownerId = 42L;

        DocumentEntity entity1 = new DocumentEntity(
                null,
                "Wrong document title",
                "Wrong document content",
                ownerId,
                DocumentStatus.FAILED
        );

        DocumentEntity entity2 = new DocumentEntity(
                null,
                "Right document title",
                "Right document content",
                ownerId,
                DocumentStatus.READY
        );

        em.persist(entity1);
        em.persist(entity2);
        em.flush();

        Long id = entity2.getId();

        Optional<Document> result = repository.findByIdAndOwnerId(id, ownerId);

        assertThat(result).isPresent();

        Document found = result.orElseThrow();

        assertThat(found)
                .extracting(
                        Document::getId,
                        Document::getTitle,
                        Document::getContent,
                        Document::getOwnerId,
                        Document::getStatus
                )
                .containsExactly(
                        id,
                        "Right document title",
                        "Right document content",
                        ownerId,
                        DocumentStatus.READY
                );
    }

    @Test
    void findByIdAndOwnerId_whenNotFound_shouldReturnEmpty() {
        Long id = 999L;
        Long ownerId = 42L;

        Optional<Document> result = repository.findByIdAndOwnerId(id, ownerId);

        assertThat(result).isEmpty();
    }
}
