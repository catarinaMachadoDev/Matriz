package dev.matriz.tasks;
import java.util.*;
import dev.matriz.shared.PageResponse;
import java.security.Principal;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import static dev.matriz.shared.Data.*;
@RestController @RequestMapping("/api/projects/{projectId}/tasks")
public class TaskController {
 private final TaskService service;
 public TaskController(TaskService service){this.service=service;}
 UUID user(Principal p){return service.user(p.getName());}
 public record TaskInput(String title,String description,String status,String priority,Long version){}
 public record Assignment(String userId){}
 public record CommentInput(String content){}
 @GetMapping public PageResponse<TaskResponse> list(Principal a,@PathVariable String projectId,@RequestParam(defaultValue="")String status,@RequestParam(defaultValue="")String priority,@RequestParam(defaultValue="")String assigneeId,@RequestParam(defaultValue="20")int limit,@RequestParam(defaultValue="0")int offset){return service.list(id(projectId),user(a),status,priority,assigneeId,limit,offset);}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public TaskResponse create(Principal a,@PathVariable String projectId,@RequestBody TaskInput b){return TaskResponse.from(service.create(id(projectId),user(a),b.title(),b.description(),b.status(),b.priority()));}
 @GetMapping("/{taskId}") public TaskResponse get(Principal a,@PathVariable String projectId,@PathVariable String taskId){return TaskResponse.from(service.get(id(projectId),user(a),id(taskId)));}
 @PutMapping("/{taskId}") public TaskResponse update(Principal a,@PathVariable String projectId,@PathVariable String taskId,@RequestBody TaskInput b){return TaskResponse.from(service.update(id(projectId),user(a),id(taskId),b.title(),b.description(),b.status(),b.priority(),b.version()));}
 @DeleteMapping("/{taskId}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(Principal a,@PathVariable String projectId,@PathVariable String taskId,@RequestParam long version){service.delete(id(projectId),user(a),id(taskId),version);}
 @PostMapping("/{taskId}/assignees") @ResponseStatus(HttpStatus.NO_CONTENT) public void assign(Principal a,@PathVariable String projectId,@PathVariable String taskId,@RequestBody Assignment b){service.assign(id(projectId),user(a),id(taskId),id(b.userId()));}
 @DeleteMapping("/{taskId}/assignees/{userId}") @ResponseStatus(HttpStatus.NO_CONTENT) public void unassign(Principal a,@PathVariable String projectId,@PathVariable String taskId,@PathVariable String userId){service.unassign(id(projectId),user(a),id(taskId),id(userId));}
 @GetMapping("/{taskId}/comments") public List<Map<String,Object>> comments(Principal a,@PathVariable String projectId,@PathVariable String taskId){return service.comments(id(projectId),user(a),id(taskId));}
 @PostMapping("/{taskId}/comments") @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> comment(Principal a,@PathVariable String projectId,@PathVariable String taskId,@RequestBody CommentInput b){return service.comment(id(projectId),user(a),id(taskId),b.content());}
}
