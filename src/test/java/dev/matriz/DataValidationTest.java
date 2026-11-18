package dev.matriz;

import dev.matriz.shared.ApiError.Failure;
import dev.matriz.shared.Data;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class DataValidationTest {
    @ParameterizedTest
    @CsvSource({"name,100", "title,120", "description,2000", "content,2000"})
    void validatesRequiredTextAfterTrimming(String field, int limit) {
        String boundary = "x".repeat(limit - 1) + "\uD83D\uDE42";
        assertEquals(boundary, Data.text("  " + boundary + "\n", field, limit, true));
        assertEquals(field, assertThrows(Failure.class,
            () -> Data.text(boundary + "x", field, limit, true)).fields.keySet().iterator().next());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  ", "\t\n", "\u2003"})
    void rejectsBlankRequiredTextAndNormalizesOptionalText(String value) {
        assertEquals(400, assertThrows(Failure.class, () -> Data.text(value, "title", 120, true)).status);
        assertEquals("", Data.text(value, "description", 1000, false));
    }

    @Test
    void validatesOptionalDescriptionAndRejectsNullCharacters() {
        String boundary = "x".repeat(999) + "\uD83D\uDE42";
        assertEquals(boundary, Data.text(boundary, "description", 1000, false));
        assertEquals(400, assertThrows(Failure.class,
            () -> Data.text("x".repeat(1001), "description", 1000, false)).status);
        assertEquals(400, assertThrows(Failure.class,
            () -> Data.text("A\0B", "title", 120, true)).status);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"invalid", "1-2-3-4-5", "00000001-0002-0003-0004-000000000005 "})
    void rejectsMalformedIdentifiers(String value) {
        assertEquals(400, assertThrows(Failure.class, () -> Data.id(value)).status);
    }

    @Test
    void acceptsCanonicalIdentifiersInEitherCase() {
        var expected = UUID.fromString("a12b34cd-1234-5678-9abc-4abc5678def0");
        assertEquals(expected, Data.id(expected.toString()));
        assertEquals(expected, Data.id("A12B34CD-1234-5678-9ABC-4ABC5678DEF0"));
    }

    @ParameterizedTest
    @CsvSource({"1,0", "20,0", "100,2147483647", "20,30"})
    void acceptsPaginationBoundaries(int limit, int offset) {
        assertDoesNotThrow(() -> Data.page(limit, offset));
    }

    @ParameterizedTest
    @CsvSource({"0,0", "-1,0", "101,0", "20,-1"})
    void rejectsOutOfRangePagination(int limit, int offset) {
        assertEquals(400, assertThrows(Failure.class, () -> Data.page(limit, offset)).status);
    }

    @ParameterizedTest
    @ValueSource(strings = {"todo", "in_progress", "done"})
    void acceptsEveryStatus(String value) {
        assertEquals(value, Data.choice(value, "status", "todo", "in_progress", "done"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"DONE", "done ", "closed"})
    void rejectsInvalidStatus(String value) {
        var error = assertThrows(Failure.class,
            () -> Data.choice(value, "status", "todo", "in_progress", "done"));
        assertEquals(400, error.status);
        assertTrue(error.fields.containsKey("status"));
    }

    @Test
    void validatesVersionsAndAffectedRowCount() {
        Data.version(0L);
        Data.version(Long.MAX_VALUE);
        Data.changed(1);
        assertEquals(400, assertThrows(Failure.class, () -> Data.version(null)).status);
        assertEquals(400, assertThrows(Failure.class, () -> Data.version(-1L)).status);
        assertEquals(409, assertThrows(Failure.class, () -> Data.changed(0)).status);
        assertEquals(409, assertThrows(Failure.class, () -> Data.changed(2)).status);
    }
}
