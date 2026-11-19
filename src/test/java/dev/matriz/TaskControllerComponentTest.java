package dev.matriz;

import dev.matriz.shared.ApiError;
import dev.matriz.shared.PageResponse;
import dev.matriz.tasks.TaskController;
import dev.matriz.tasks.TaskService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TaskControllerComponentTest {
    private final TaskService tasks = mock(TaskService.class);
    private final UUID user = UUID.randomUUID();
    private final UUID project = UUID.randomUUID();
    private final UUID task = UUID.randomUUID();
    private final String base = "/api/projects/" + project + "/tasks";
    private MockMvc mvc;

    @BeforeEach
    void configureMvc() {
        mvc = MockMvcBuilders.standaloneSetup(new TaskController(tasks))
            .setControllerAdvice(new ApiError()).build();
    }

    @Test
    void listBindsDefaultsAndExplicitFilters() throws Exception {
        when(tasks.user("sam")).thenReturn(user);
        when(tasks.list(project, user, "", "", "", 20, 0))
            .thenReturn(new PageResponse<>(List.of(), 0, 20, 0));
        when(tasks.list(project, user, "done", "high", user.toString(), 5, 10))
            .thenReturn(new PageResponse<>(List.of(), 12, 5, 10));

        mvc.perform(get(base).principal(() -> "sam"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.items").isEmpty())
            .andExpect(jsonPath("$.limit").value(20)).andExpect(jsonPath("$.offset").value(0));
        mvc.perform(get(base).principal(() -> "sam")
                .param("status", "done").param("priority", "high").param("assigneeId", user.toString()).param("limit", "5").param("offset", "10"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(12))
            .andExpect(jsonPath("$.offset").value(10));
        verify(tasks).list(project, user, "done", "high", user.toString(), 5, 10);
    }

    @Test
    void malformedJsonPathAndVersionAreRejectedBeforeCallingService() throws Exception {
        mvc.perform(post(base).principal(() -> "sam").contentType("application/json").content("{"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        mvc.perform(get("/api/projects/1-2-3-4-5/tasks").principal(() -> "sam"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("HTTP_400"));
        mvc.perform(delete(base + "/" + task).principal(() -> "sam").param("version", "bad"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        verifyNoInteractions(tasks);
    }

    @Test
    void createReturnsThePublicResponseAndHidesInternalFields() throws Exception {
        when(tasks.user("sam")).thenReturn(user);
        when(tasks.create(project, user, "Title", "Details", null, null)).thenReturn(Map.of(
            "id", task, "projectId", project, "title", "Title", "description", "Details",
            "status", "todo", "priority", "medium", "version", 0L, "assignees", List.of(),
            "createdAt", "internal"));
        mvc.perform(post(base).principal(() -> "sam").contentType("application/json")
                .content("{\"title\":\"Title\",\"description\":\"Details\"}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.id").value(task.toString()))
            .andExpect(jsonPath("$.projectId").value(project.toString()))
            .andExpect(jsonPath("$.status").value("todo")).andExpect(jsonPath("$.assignees").isEmpty())
            .andExpect(jsonPath("$.createdAt").doesNotExist());
    }

    @Test
    void adviceMapsDomainValidationAndConflict() throws Exception {
        when(tasks.user("sam")).thenReturn(user);
        when(tasks.create(project, user, "Title", null, null, null)).thenThrow(
            new ApiError.Failure(400, "Check the highlighted fields.", Map.of("description", "Required")));
        doThrow(new ApiError.Failure(409, "This record was changed by someone else."))
            .when(tasks).delete(project, user, task, 0);

        mvc.perform(post(base).principal(() -> "sam").contentType("application/json")
                .content("{\"title\":\"Title\"}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.description").value("Required"));
        mvc.perform(delete(base + "/" + task).principal(() -> "sam").param("version", "0"))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("HTTP_409"));
    }
}
