package com.example.platform.kernel.ui;

import com.example.platform.kernel.exception.BusinessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ApiPaginationSupport {

    public static final int MAX_PAGE_SIZE = 100;

    private ApiPaginationSupport() {
    }

    public static Pageable pageRequest(int page, int size, Sort sort) {
        if (page < 0) {
            throw new BusinessException("page must be greater than or equal to 0");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessException("size must be between 1 and " + MAX_PAGE_SIZE);
        }
        return PageRequest.of(page, size, sort);
    }

    public static Map<String, Object> pageMeta(Page<?> page) {
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("page", page.getNumber());
        meta.put("size", page.getSize());
        meta.put("totalItems", page.getTotalElements());
        meta.put("totalPages", page.getTotalPages());
        meta.put("hasNext", page.hasNext());
        meta.put("hasPrevious", page.hasPrevious());
        return meta;
    }
}
