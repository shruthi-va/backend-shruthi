package com.chubb.claims.claim.adapter.in.web.dto;

import com.chubb.claims.claim.application.query.PageResult;
import java.util.List;
import java.util.function.Function;

public record PageResponse<T>(List<T> items, int page, int size, long totalElements) {

    public static <S, T> PageResponse<T> of(PageResult<S> result, Function<S, T> mapper) {
        return new PageResponse<>(result.items().stream().map(mapper).toList(), result.page(), result.size(),
                result.totalElements());
    }
}
