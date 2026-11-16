package dev.matriz.shared;

import java.util.List;

public record PageResponse<T>(List<T> items, long total, int limit, int offset) {}
