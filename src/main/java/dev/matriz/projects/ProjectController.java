package dev.matriz.projects;
import java.util.*;
import java.security.Principal;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import static dev.matriz.shared.Data.*;
@RestController @RequestMapping("/api/projects")
public class ProjectController {
 private final ProjectService service;
 public ProjectController(ProjectService service){this.service=service;}
 public record ProjectInput(String name,String description,Long version){}
 public record MemberInput(String userId){}
 @GetMapping public Map<String,Object> list(Principal auth,@RequestParam(defaultValue="")String search,@RequestParam(defaultValue="20")int limit,@RequestParam(defaultValue="0")int offset){page(limit,offset);var user=service.user(auth.getName());String q="%"+text(search,"search",100,false).toLowerCase().replace("!","!!").replace("%","!%").replace("_","!_")+"%";String from=" from project p join project_member m on m.project_id=p.id where m.user_id=? and lower(p.name) like ? escape '!'";var rows=service.data().rows("select p.*"+from+" order by p.id limit ? offset ?",user,q,limit,offset);for(var p:rows)p.put("counts",service.counts((UUID)p.get("id"),user));return Map.of("items",rows,"total",service.data().jdbc.queryForObject("select count(*)"+from,Long.class,user,q),"limit",limit,"offset",offset);}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public Map<String,Object> create(Principal auth,@RequestBody ProjectInput p){return service.create(service.user(auth.getName()),p.name(),p.description());}
 @GetMapping("/{projectId}") public Map<String,Object> get(Principal a,@PathVariable String projectId){var u=service.user(a.getName());var p=service.access(id(projectId),u);p.put("counts",service.counts(id(projectId),u));return p;}
 @PutMapping("/{projectId}") public Map<String,Object> update(Principal a,@PathVariable String projectId,@RequestBody ProjectInput p){return service.update(id(projectId),service.user(a.getName()),p.name(),p.description(),p.version());}
 @DeleteMapping("/{projectId}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(Principal a,@PathVariable String projectId,@RequestParam long version){service.delete(id(projectId),service.user(a.getName()),version);}
 @GetMapping("/{projectId}/members") public List<Map<String,Object>> members(Principal a,@PathVariable String projectId){return service.members(id(projectId),service.user(a.getName()));}
 @GetMapping("/{projectId}/eligible-users") public List<Map<String,Object>> eligible(Principal a,@PathVariable String projectId){service.manager(id(projectId),service.user(a.getName()));return service.data().rows("select id,username,display_name from app_user order by username");}
 @PostMapping("/{projectId}/members") @ResponseStatus(HttpStatus.NO_CONTENT) public void add(Principal a,@PathVariable String projectId,@RequestBody MemberInput p){service.addMember(id(projectId),service.user(a.getName()),id(p.userId()));}
}
