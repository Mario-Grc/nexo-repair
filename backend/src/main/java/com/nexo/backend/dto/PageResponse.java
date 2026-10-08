package com.nexo.backend.dto;

import java.util.List;

import org.springframework.data.domain.Page;

// Stable pagination envelope. A custom record is used because
// the JSON shape of PageImpl is not guaranteed across versions.
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
