package dev.matriz;

import dev.matriz.shared.ApiError.Failure;
import dev.matriz.shared.Data;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TaskTitleTest {
    @Test
    void trimsValidTitle() {
        assertEquals("Plan release", Data.text("  Plan release  ", "title", 120, true));
    }

    @Test
    void rejectsBlankTitle() {
        Failure error = assertThrows(Failure.class, () -> Data.text(" \t ", "title", 120, true));
        assertEquals(400, error.status);
        assertTrue(error.fields.containsKey("title"));
    }

    @Test
    void accepts120CharactersAndRejects121() {
        String boundary = "x".repeat(120);
        assertEquals(boundary, Data.text(boundary, "title", 120, true));
        assertThrows(Failure.class, () -> Data.text(boundary + "x", "title", 120, true));
    }
}
