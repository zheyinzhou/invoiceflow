package com.zzy.domain.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record PageResp<T>(
        List<T> content,
        long totalElements,
        int totalPages,
        int size,
        int number
) {
    public static <T> PageResp<T> of(Page<T> p) {
        return new PageResp<>(
                p.getContent(),
                p.getTotalElements(),
                p.getTotalPages(),
                p.getSize(),
                p.getNumber()
        );
    }
}
