package dev.matriz.projects;
import dev.matriz.shared.Data;
import static dev.matriz.shared.Data.*;
import static dev.matriz.shared.ApiError.Failure;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class ProjectService {
 private final Data db;
 public Data data(){return db;}
 public ProjectService(Data db){this.db=db;}
 public UUID user(String username){return (UUID)db.one("select id from app_user where username=?",username).get("id");}
 public Map<String,Object> access(UUID project,UUID user){return db.one("select p.* from project p join project_member m on m.project_id=p.id where p.id=? and m.user_id=?",project,user);}
 public Map<String,Object> manager(UUID project,UUID user){var p=access(project,user);if(!p.get("managerId").equals(user))throw new Failure(403,"Only the project manager may perform this action.");return p;}
 @Transactional public Map<String,Object> create(UUID user,String name,String description){var id=UUID.randomUUID();db.jdbc.update("insert into project(id,name,description,manager_id) values(?,?,?,?)",id,text(name,"name",100,true),text(description,"description",1000,false),user);db.jdbc.update("insert into project_member values(?,?)",id,user);return access(id,user);}
 @Transactional public Map<String,Object> update(UUID project,UUID user,String name,String description,Long version){manager(project,user);version(version);changed(db.jdbc.update("update project set name=?,description=?,version=version+1 where id=? and version=?",text(name,"name",100,true),text(description,"description",1000,false),project,version));return access(project,user);}
 @Transactional public void delete(UUID project,UUID user,long version){manager(project,user);version(version);changed(db.jdbc.update("delete from project where id=? and version=?",project,version));}
 public List<Map<String,Object>> members(UUID project,UUID user){access(project,user);return db.rows("select u.id,u.username,u.display_name from app_user u join project_member m on m.user_id=u.id where m.project_id=? order by u.username",project);}
 @Transactional public void addMember(UUID project,UUID user,UUID member){manager(project,user);if(db.rows("select id from app_user where id=?",member).isEmpty())throw new Failure(400,"User does not exist.");db.jdbc.update("insert into project_member values(?,?) on conflict do nothing",project,member);}
 public Map<String,Object> counts(UUID project,UUID user){access(project,user);var counts=new LinkedHashMap<String,Object>();long total=0,done=0;for(String s:List.of("todo","in_progress","done")){long n=db.jdbc.queryForObject("select count(*) from task where project_id=? and status=?",Long.class,project,s);counts.put(s,n);total+=n;if(s.equals("done"))done=n;}counts.put("total",total);counts.put("percentage",total==0?0:Math.round(done*1000.0/total)/10.0);return counts;}
}
