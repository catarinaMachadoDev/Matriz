package dev.matriz.projects;
import java.util.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import dev.matriz.shared.PageResponse;
import dev.matriz.projects.ProjectService.TaskCounts;
import java.security.Principal;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import static dev.matriz.shared.Data.*;
@RestController @RequestMapping("/api/projects")
public class ProjectController {
 private final ProjectService service;
 public ProjectController(ProjectService service){this.service=service;}
 public record ProjectInput(String name,String description,Long version){}
 public record ProjectResponse(UUID id,String name,String description,UUID managerId,long version,
                               @JsonInclude(JsonInclude.Include.NON_NULL) TaskCounts counts){
  static ProjectResponse from(Map<String,Object> row,TaskCounts counts){
   return new ProjectResponse((UUID)row.get("id"),(String)row.get("name"),(String)row.get("description"),
       (UUID)row.get("managerId"),(long)row.get("version"),counts);
  }
 }
 public record MemberInput(String userId){}
 @GetMapping public PageResponse<ProjectResponse> list(Principal auth,@RequestParam(defaultValue="")String search,@RequestParam(defaultValue="20")int limit,@RequestParam(defaultValue="0")int offset){page(limit,offset);var user=service.user(auth.getName());String q="%"+text(search,"search",100,false).toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_")+"%";String from=" from project p join project_member m on m.project_id=p.id where m.user_id=? and lower(p.name) like ? escape '!'";var rows=service.data().rows("select p.*"+from+" order by p.id limit ? offset ?",user,q,limit,offset);var counts=service.counts(rows.stream().map(p->(UUID)p.get("id")).toList());var items=rows.stream().map(p->ProjectResponse.from(p,service.countsOrEmpty(counts,(UUID)p.get("id")))).toList();return new PageResponse<>(items,service.data().jdbc.queryForObject("select count(*)"+from,Long.class,user,q),limit,offset);}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public ProjectResponse create(Principal auth,@RequestBody ProjectInput p){return ProjectResponse.from(service.create(service.user(auth.getName()),p.name(),p.description()),null);}
 @GetMapping("/{projectId}") public ProjectResponse get(Principal a,@PathVariable String projectId){var u=service.user(a.getName());var p=service.access(id(projectId),u);var project=id(projectId);return ProjectResponse.from(p,service.countsOrEmpty(service.counts(List.of(project)),project));}
 @PutMapping("/{projectId}") public ProjectResponse update(Principal a,@PathVariable String projectId,@RequestBody ProjectInput p){return ProjectResponse.from(service.update(id(projectId),service.user(a.getName()),p.name(),p.description(),p.version()),null);}
 @DeleteMapping("/{projectId}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(Principal a,@PathVariable String projectId,@RequestParam long version){service.delete(id(projectId),service.user(a.getName()),version);}
 @GetMapping("/{projectId}/members") public List<Map<String,Object>> members(Principal a,@PathVariable String projectId){return service.members(id(projectId),service.user(a.getName()));}
 @GetMapping("/{projectId}/eligible-users") public List<Map<String,Object>> eligible(Principal a,@PathVariable String projectId){service.manager(id(projectId),service.user(a.getName()));return service.data().rows("select id,username,display_name from app_user order by username");}
 @PostMapping("/{projectId}/members") @ResponseStatus(HttpStatus.NO_CONTENT) public void add(Principal a,@PathVariable String projectId,@RequestBody MemberInput p){service.addMember(id(projectId),service.user(a.getName()),id(p.userId()));}
}
