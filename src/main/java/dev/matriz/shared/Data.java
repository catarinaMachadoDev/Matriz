package dev.matriz.shared;

import java.util.*;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import static dev.matriz.shared.ApiError.Failure;

@Component
public class Data {
    public final JdbcTemplate jdbc;

    public Data(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> rows(String sql, Object... args) {
        return jdbc.queryForList(sql, args).stream().map(row -> {
            Map<String, Object> out = new LinkedHashMap<>();
            row.forEach((k, v) -> {
                String[] parts = k.split("_");
                String key = parts[0];
                for (int i = 1; i < parts.length; i++)
                    key += Character.toUpperCase(parts[i].charAt(0)) + parts[i].substring(1);
                out.put(key, v instanceof java.sql.Timestamp t ? t.toInstant().toString() : v);
            });
            return out;
        }).toList();
    }

    public Map<String, Object> one(String sql, Object... args) {
        var list = rows(sql, args);
        if (list.isEmpty()) throw new Failure(404, "Resource not found.");
        return list.getFirst();
    }

    public static UUID id(String value) {
        if (value == null || value.length() != 36) throw new Failure(400, "Invalid identifier.");
        try {
            UUID parsed = UUID.fromString(value);
            if (parsed.toString().equalsIgnoreCase(value)) return parsed;
        } catch (IllegalArgumentException e) {
            throw new Failure(400, "Invalid identifier.");
        }
        throw new Failure(400, "Invalid identifier.");
    }

    public static String text(String value, String field, int max, boolean required) {
        String v = value == null ? "" : value.strip();
        if (v.indexOf(0) >= 0)
            throw new Failure(400, "Check the highlighted fields.", Map.of(field, "Must not contain a NUL character."));
        if (v.codePointCount(0, v.length()) > max || (required && v.isBlank()))
            throw new Failure(400, "Check the highlighted fields.", Map.of(field, "Must contain " + (required ? "1" : "0") + "–" + max + " characters."));
        return v;
    }

    public static String choice(String value, String field, String... allowed) {
        if (!Arrays.asList(allowed).contains(value))
            throw new Failure(400, "Invalid " + field + ".", Map.of(field, "Choose a valid value."));
        return value;
    }

    public static void page(int limit, int offset) {
        if (limit < 1 || limit > 100 || offset < 0)
            throw new Failure(400, "Limit must be 1–100 and offset must be nonnegative.");
    }

    public static void version(Long version) {
        if (version == null || version < 0) throw new Failure(400, "A valid version is required.");
    }

    public static void changed(int count) {
        if (count != 1)
            throw new Failure(409, "This record was changed by someone else. Refresh before editing again.");
    }
}
