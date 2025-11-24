package com.github.stepanterkun.searchengine.document.api.controller;

import com.github.stepanterkun.searchengine.document.api.dto.DocumentCreateDto;
import com.github.stepanterkun.searchengine.document.api.dto.DocumentDto;
import com.github.stepanterkun.searchengine.document.domain.model.DocumentNotFoundException;
import com.github.stepanterkun.searchengine.document.domain.service.DocumentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller for working with documents of the current user.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>accept HTTP requests,</li>
 *   <li>read {@code ownerId} from the header,</li>
 *   <li>delegate work to {@link DocumentService},</li>
 *   <li>wrap responses into {@link ResponseEntity}.</li>
 * </ul>
 * All business logic lives in the service layer.
 */
@RestController
@RequestMapping("/documents")
public class DocumentController {

    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);

    private final DocumentService service;

    public DocumentController(DocumentService service) {
        this.service = service;
    }

    /**
     * Creates a new document for the given owner.
     *
     * @param ownerId   id of the owner (header {@code X-User-Id})
     * @param createDto document data
     * @return created document
     */
    @PostMapping
    public ResponseEntity<DocumentDto> create(
            @RequestHeader("X-User-Id") @NotNull Long ownerId,
            @RequestBody @Valid DocumentCreateDto createDto
    ) {
        log.info("Create document: ownerId={}", ownerId);

        DocumentDto created = service.create(ownerId, createDto);

        return ResponseEntity
                       .status(HttpStatus.CREATED)
                       .body(created);
    }

    /**
     * Returns all documents of the given owner.
     */
    @GetMapping("/all")
    public ResponseEntity<List<DocumentDto>> getAllForOwner(
            @RequestHeader("X-User-Id") @NotNull Long ownerId
    ) throws DocumentNotFoundException {
        log.info("Get all documents: ownerId={}", ownerId);

        List<DocumentDto> documents = service.getAllForOwner(ownerId);

        return ResponseEntity.ok(documents);
    }

    /**
     * Returns a single document by id for the given owner.
     */
    @GetMapping("/{id}")
    public ResponseEntity<DocumentDto> getByIdForOwner(
            @PathVariable("id") @NotNull Long id,
            @RequestHeader("X-User-Id") @NotNull Long ownerId
    ) throws DocumentNotFoundException {
        log.info("Get document by id: id={}, ownerId={}", id, ownerId);

        DocumentDto document = service.getByIdForOwner(id, ownerId);

        return ResponseEntity.ok(document);
    }

    /**
     * Deletes a single document by id for the given owner.
     * <p>
     * Errors like {@link DocumentNotFoundException} are handled by GlobalExceptionHandler.
     */
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteByIdForOwner(
            @PathVariable("id") @NotNull Long id,
            @RequestHeader("X-User-Id") @NotNull Long ownerId
    ) throws DocumentNotFoundException {
        log.info("Delete document: id={}, ownerId={}", id, ownerId);

        service.deleteByIdForOwner(id, ownerId);

        // return 200 OK with empty body
        return ResponseEntity
                       .ok()
                       .build();
    }

    /**
     * Deletes all documents for the given owner.
     */
    @DeleteMapping("/delete/all")
    public ResponseEntity<Void> deleteAllForOwner(
            @RequestHeader("X-User-Id") @NotNull Long ownerId
    ) {
        log.info("Delete all documents: ownerId={}", ownerId);

        service.deleteAllForOwner(ownerId);

        // return 200 OK with empty body
        return ResponseEntity
                       .ok()
                       .build();
    }
}
