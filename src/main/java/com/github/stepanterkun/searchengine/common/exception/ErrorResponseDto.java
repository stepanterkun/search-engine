package com.github.stepanterkun.searchengine.common.exception;

import java.time.Instant;

public record ErrorResponseDto (
        String code,
        String message,
        Instant timestamp
) {
}
