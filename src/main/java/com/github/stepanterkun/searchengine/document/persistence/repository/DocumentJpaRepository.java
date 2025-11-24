package com.github.stepanterkun.searchengine.document.persistence.repository;

import com.github.stepanterkun.searchengine.document.persistence.entity.DocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface DocumentJpaRepository
        extends JpaRepository<DocumentEntity, Long> {

    Optional<DocumentEntity> findByIdAndOwnerId(
            Long id,
            Long ownerId
    );

    List<DocumentEntity> findAllByOwnerId(Long ownerId);

    void deleteByIdAndOwnerId(Long id, Long ownerId);


}
