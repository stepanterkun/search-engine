package com.github.stepanterkun.searchengine.document.api.dto;

import com.github.stepanterkun.searchengine.document.domain.model.DocumentStatus;

public record DocumentDto(
        Long id,
        String title,
        DocumentStatus status
) {
}
