package dev.matriz.tasks;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record TaskResponse(UUID id, UUID projectId, String title, String description, String status, String priority,
                           long version, List<Assignee> assignees) {
    @SuppressWarnings("unchecked")
    static TaskResponse from(Map<String, Object> row) {
        var people = (List<Map<String, Object>>) row.get("assignees");
        return new TaskResponse((UUID) row.get("id"), (UUID) row.get("projectId"), (String) row.get("title"),
                (String) row.get("description"), (String) row.get("status"), (String) row.get("priority"),
                (long) row.get("version"), people.stream().map(Assignee::from).toList());
    }

    record Assignee(UUID id, String username, String displayName) {
        static Assignee from(Map<String, Object> row) {
            return new Assignee((UUID) row.get("id"), (String) row.get("username"), (String) row.get("displayName"));
        }
    }
}
