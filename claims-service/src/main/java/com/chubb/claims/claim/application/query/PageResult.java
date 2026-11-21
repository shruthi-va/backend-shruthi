package com.chubb.claims.claim.application.query;

import java.util.List;

public record PageResult<T>(List<T> items, int page, int size, long totalElements) {
}
