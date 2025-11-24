package com.github.stepanterkun.searchengine.document.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for creating a new document.
 * <p>
 * Both fields are required and have length limits.
 */
public record DocumentCreateDto(
        @NotBlank(message = "Title must not be blank")
        @Size(
                max = 100,
                message = "Title must be 100 characters at most"
        )
        String title,

        @NotBlank(message = "Content must not be blank")
        @Size(
                max = 100_000,
                message = "Content is too large: must be 100000 characters at most"
        )
        String content
) {
}
