package com.ndzela.admin.common.web;

import com.ndzela.admin.common.exception.InvalidParameterException;

import org.springframework.data.domain.Sort;

import java.util.Locale;
import java.util.Map;

/** Reads a sort=field,asc|desc parameter against a whitelist of public names. */
public final class SortParam {
    private SortParam() {}

    public static Sort parse(String raw, Map<String, String> allowed, Sort fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String[] parts = raw.trim().split(",");
        String property = allowed.get(parts[0].trim());
        if (property == null || parts.length > 2) {
            throw new InvalidParameterException("sort");
        }
        Sort.Direction direction = Sort.Direction.ASC;
        if (parts.length == 2) {
            switch (parts[1].trim().toLowerCase(Locale.ROOT)) {
                case "asc" -> direction = Sort.Direction.ASC;
                case "desc" -> direction = Sort.Direction.DESC;
                default -> throw new InvalidParameterException("sort");
            }
        }
        return Sort.by(direction, property).and(Sort.by(Sort.Direction.ASC, "id"));
    }
}
