package dev.matriz.tasks;
import dev.matriz.shared.Data;
import dev.matriz.shared.PageResponse;
import dev.matriz.projects.ProjectService;
import static dev.matriz.shared.Data.*;
import static dev.matriz.shared.ApiError.Failure;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class TaskService {
 private final Data db;private final ProjectService projects;
 public UUID user(String username){return projects.user(username);}
 public TaskService(Data db,ProjectService projects){this.db=db;this.projects=projects;}
 public Map<String,Object> get(UUID project,UUID user,UUID task){projects.access(project,user);var t=db.one("select * from task where id=? and project_id=?",task,project);t.put("assignees",db.rows("select u.id,u.username,u.display_name from app_user u join task_assignee a on a.user_id=u.id where a.task_id=? order by u.username",task));return t;}
 @Transactional public Map<String,Object> create(UUID project,UUID user,String title,String description){projects.access(project,user);var task=UUID.randomUUID();db.jdbc.update("insert into task(id,project_id,title,description) values(?,?,?,?)",task,project,text(title,"title",120,true),text(description,"description",2000,true));return get(project,user,task);}
 @Transactional public Map<String,Object> update(UUID project,UUID user,UUID task,String title,String description,String status,String priority,Long version){get(project,user,task);version(version);changed(db.jdbc.update("update task set title=?,description=?,status=?,priority=?,version=version+1 where id=? and version=?",text(title,"title",120,true),text(description,"description",2000,true),choice(status,"status","todo","in_progress","done"),choice(priority,"priority","low","medium","high"),task,version));return get(project,user,task);}
 @Transactional public void delete(UUID project,UUID user,UUID task,long version){get(project,user,task);version(version);changed(db.jdbc.update("delete from task where id=? and version=?",task,version));}
 @Transactional(readOnly=true, isolation=org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
 public PageResponse<TaskResponse> list(UUID project,UUID user,String status,String priority,int limit,int offset){
  projects.access(project,user);
  page(limit,offset);
  List<Object> args=new ArrayList<>();
  args.add(project);
  String where=" where project_id=?";
  if(!status.isEmpty()){where+=" and status=?";args.add(choice(status,"status","todo","in_progress","done"));}
  if(!priority.isEmpty()){where+=" and priority=?";args.add(choice(priority,"priority","low","medium","high"));}
  long count=db.jdbc.queryForObject("select count(*) from task"+where,Long.class,args.toArray());
  args.add(limit);
  args.add(offset);
  var rows=db.rows("select * from task"+where+" order by created_at desc, id desc limit ? offset ?",args.toArray());
  Map<Object,List<Map<String,Object>>> assignees=new HashMap<>();
  if(!rows.isEmpty()){
   String slots=String.join(",",Collections.nCopies(rows.size(),"?"));
   Object[] ids=rows.stream().map(row->row.get("id")).toArray();
   for(var assignee:db.rows("""
     select a.task_id,u.id,u.username,u.display_name
     from task_assignee a join app_user u on u.id=a.user_id
     where a.task_id in ("""+slots+") order by u.username",ids)){
    Object taskId=assignee.remove("taskId");
    assignees.computeIfAbsent(taskId,key->new ArrayList<>()).add(assignee);
   }
  }
  for(var row:rows)row.put("assignees",assignees.getOrDefault(row.get("id"),List.of()));
  return new PageResponse<>(rows.stream().map(TaskResponse::from).toList(),count,limit,offset);
 }
 @Transactional public void assign(UUID project,UUID user,UUID task,UUID assignee){get(project,user,task);if(db.rows("select user_id from project_member where project_id=? and user_id=?",project,assignee).isEmpty())throw new Failure(400,"Assignee must belong to this project.");db.jdbc.update("insert into task_assignee values(?,?,?) on conflict do nothing",task,project,assignee);}
 @Transactional public void unassign(UUID project,UUID user,UUID task,UUID assignee){get(project,user,task);db.jdbc.update("delete from task_assignee where task_id=? and user_id=?",task,assignee);}
 public List<Map<String,Object>> comments(UUID project,UUID user,UUID task){get(project,user,task);return db.rows("select c.id,c.task_id,c.author_id,u.display_name as author_name,c.content,c.created_at from task_comment c join app_user u on u.id=c.author_id where c.task_id=? order by c.created_at,c.id",task);}
 @Transactional public Map<String,Object> comment(UUID project,UUID user,UUID task,String content){get(project,user,task);var id=UUID.randomUUID();db.jdbc.update("insert into task_comment(id,task_id,author_id,content) values(?,?,?,?)",id,task,user,text(content,"content",2000,true));return db.one("select c.id,c.task_id,c.author_id,u.display_name as author_name,c.content,c.created_at from task_comment c join app_user u on u.id=c.author_id where c.id=?",id);}
}
