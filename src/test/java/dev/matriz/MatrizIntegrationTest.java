package dev.matriz;
import dev.matriz.projects.ProjectService;
import dev.matriz.tasks.TaskService;
import dev.matriz.shared.Data;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties={"matriz.demo.enabled=true","server.servlet.session.cookie.secure=false"})
@AutoConfigureMockMvc @Transactional
class MatrizIntegrationTest {
 @DynamicPropertySource static void testDatabase(DynamicPropertyRegistry r){r.add("spring.datasource.url",()->System.getenv().getOrDefault("MATRIZ_TEST_DB_URL","jdbc:postgresql://127.0.0.1:5432/matriz_test"));}
 @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired ProjectService projects; @Autowired TaskService tasks;
 @org.springframework.test.context.bean.override.mockito.MockitoSpyBean Data db;
 @Autowired org.springframework.transaction.PlatformTransactionManager transactions;
 @Autowired dev.matriz.identity.DemoSeed demoSeed;
 UUID catarina(){return projects.user("catarina");}
 UUID project(){return (UUID)projects.create(catarina(),"Test project","Verification").get("id");}
 String base(UUID p){return "/api/projects/"+p+"/tasks";}
 String body(Object v)throws Exception{return json.writeValueAsString(v);}

 @Test void demoSeedPreservesExistingAccountsAndCreatesMissingAccountsOnce() {
  UUID user = catarina();
  String passwordHash = db.jdbc.queryForObject("select password_hash from app_user where id=?", String.class, user);
  long projectCount = db.jdbc.queryForObject("select count(*) from project", Long.class);
  db.jdbc.update("update app_user set username='previous-taylor' where username='taylor'");
  db.jdbc.update("delete from demo_seed_marker where id=1");

  demoSeed.run(null);
  UUID newTaylor = projects.user("taylor");
  demoSeed.run(null);

  assertEquals(user, catarina());
  assertEquals(passwordHash, db.jdbc.queryForObject("select password_hash from app_user where id=?", String.class, user));
  assertEquals(newTaylor, projects.user("taylor"));
  assertEquals(projectCount, db.jdbc.queryForObject("select count(*) from project", Long.class));
  assertEquals(0L, db.jdbc.queryForObject("select count(*) from project_member where user_id=?", Long.class, newTaylor));
 }

 @Test @WithMockUser("catarina") void mandatoryDescriptionAndWhitespaceRejected()throws Exception{
  var p=project();for(String description:List.of("","   ","x".repeat(2001)))mvc.perform(post(base(p)).with(csrf()).contentType("application/json").content(body(Map.of("title","Valid","description",description)))).andExpect(status().isBadRequest());
  mvc.perform(post(base(p)).with(csrf()).contentType("application/json").content(body(Map.of("title","   ","description","Valid")))).andExpect(status().isBadRequest());
  assertEquals(0L,db.jdbc.queryForObject("select count(*) from task where project_id=?",Long.class,p));
 }
 @Test @WithMockUser("taylor") void directCrossProjectAccessIsHidden()throws Exception{
  var p=project();var t=(UUID)tasks.create(p,catarina(),"Private","Protected content").get("id");
  mvc.perform(get(base(p)+"/"+t)).andExpect(status().isNotFound());
  mvc.perform(post(base(p)).with(csrf()).contentType("application/json").content(body(Map.of("title","Intrusion","description","Denied")))).andExpect(status().isNotFound());
  mvc.perform(delete("/api/projects/"+p).param("version","0").with(csrf())).andExpect(status().isNotFound());
 }
 @Test @WithMockUser("catarina") void createThenReadUsesRealPostgres()throws Exception{
  var p=project();var result=mvc.perform(post(base(p)).with(csrf()).contentType("application/json").content(body(Map.of("title","  Persistent task  ","description","  Required details  ")))).andExpect(status().isCreated()).andExpect(jsonPath("$.title").value("Persistent task")).andReturn();
  String id=json.readTree(result.getResponse().getContentAsString()).get("id").asText();
  assertEquals("Required details",db.jdbc.queryForObject("select description from task where id=?",String.class,UUID.fromString(id)));
  mvc.perform(get(base(p)+"/"+id)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("todo")).andExpect(jsonPath("$.priority").value("medium")).andExpect(jsonPath("$.assignees").isEmpty());
 }
 @Test @WithMockUser("catarina") void staleTaskEditReturnsConflict()throws Exception{
  var p=project();var t=(UUID)tasks.create(p,catarina(),"Task","Details").get("id");var b=Map.of("title","Updated","description","Updated details","status","done","priority","high","version",0);
  mvc.perform(put(base(p)+"/"+t).with(csrf()).contentType("application/json").content(body(b))).andExpect(status().isOk());
  mvc.perform(put(base(p)+"/"+t).with(csrf()).contentType("application/json").content(body(b))).andExpect(status().isConflict());
 }
 @Test @WithMockUser("catarina") void multipleAssigneesDeduplicateAndRequireMembership()throws Exception{
  var p=project();var sam=projects.user("sam");projects.addMember(p,catarina(),sam);var t=(UUID)tasks.create(p,catarina(),"Task","Details").get("id");
  for(var u:List.of(catarina(),sam,sam))mvc.perform(post(base(p)+"/"+t+"/assignees").with(csrf()).contentType("application/json").content(body(Map.of("userId",u)))).andExpect(status().isNoContent());
  mvc.perform(get(base(p)+"/"+t)).andExpect(jsonPath("$.assignees.length()").value(2));
  mvc.perform(post(base(p)+"/"+t+"/assignees").with(csrf()).contentType("application/json").content(body(Map.of("userId",projects.user("taylor"))))).andExpect(status().isBadRequest());
  mvc.perform(delete(base(p)+"/"+t+"/assignees/"+sam).with(csrf())).andExpect(status().isNoContent());
  mvc.perform(get(base(p)+"/"+t)).andExpect(jsonPath("$.assignees.length()").value(1));
 }
 @Test @WithMockUser("catarina") void commentAuthorAndCascadeDeletion()throws Exception{
  var p=project();var t=(UUID)tasks.create(p,catarina(),"Task","Details").get("id");tasks.assign(p,catarina(),t,catarina());
  mvc.perform(post(base(p)+"/"+t+"/comments").with(csrf()).contentType("application/json").content(body(Map.of("content","Useful comment")))).andExpect(status().isCreated()).andExpect(jsonPath("$.authorId").value(catarina().toString())).andExpect(jsonPath("$.createdAt").exists());
  mvc.perform(delete("/api/projects/"+p).param("version","0").with(csrf())).andExpect(status().isNoContent());
  for(String table:List.of("task_comment","task_assignee"))assertEquals(0L,db.jdbc.queryForObject("select count(*) from "+table+" where task_id=?",Long.class,t));
 }
 @Test @WithMockUser("sam") void memberCannotManageProjectButCanEditTasks()throws Exception{
  var p=project();projects.addMember(p,catarina(),projects.user("sam"));
  mvc.perform(put("/api/projects/"+p).with(csrf()).contentType("application/json").content(body(Map.of("name","Forbidden","description","","version",0)))).andExpect(status().isForbidden());
  mvc.perform(post(base(p)).with(csrf()).contentType("application/json").content(body(Map.of("title","Member task","description","Allowed")))).andExpect(status().isCreated());
 }
 @Test @WithMockUser("catarina") void filtersAndCountsCoverAllPages()throws Exception{
  var p=project();for(int i=0;i<3;i++){var t=(UUID)tasks.create(p,catarina(),"Task "+i,"Details").get("id");if(i<2)tasks.update(p,catarina(),t,"Task "+i,"Details","done","high",0L);}
  mvc.perform(get(base(p)).param("status","done").param("priority","high").param("limit","1")).andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.total").value(2));
  mvc.perform(get("/api/projects/"+p)).andExpect(jsonPath("$.counts.total").value(3)).andExpect(jsonPath("$.counts.done").value(2)).andExpect(jsonPath("$.counts.percentage").value(66.7));
 }
 @Test @WithMockUser("catarina") void duplicateMembershipReportsConflictWithoutCreatingAnotherRow()throws Exception{
  var p=project();var sam=projects.user("sam");var path="/api/projects/"+p+"/members";
  var candidates="/api/projects/"+p+"/eligible-users";
  mvc.perform(get(candidates)).andExpect(status().isOk())
      .andExpect(jsonPath("$[?(@.username == 'sam')]").isNotEmpty())
      .andExpect(jsonPath("$[?(@.username == 'catarina')]").isEmpty());
  mvc.perform(post(path).with(csrf()).contentType("application/json").content(body(Map.of("userId",sam))))
      .andExpect(status().isNoContent());
  mvc.perform(post(path).with(csrf()).contentType("application/json").content(body(Map.of("userId",sam))))
      .andExpect(status().isConflict()).andExpect(jsonPath("$.message").value("This user is already a project member."));
  mvc.perform(get(candidates)).andExpect(status().isOk())
      .andExpect(jsonPath("$[?(@.username == 'sam')]").isEmpty());
  assertEquals(1L,db.jdbc.queryForObject("select count(*) from project_member where project_id=? and user_id=?",Long.class,p,sam));
 }
 @Test @WithMockUser("catarina")
 @Transactional(propagation=org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
 void projectPageRetainsOneSnapshotDuringConcurrentDeletion() throws Exception {
  String name="Concurrent page "+UUID.randomUUID();
  UUID id=(UUID)projects.create(catarina(),name,"Snapshot verification").get("id");
  var separateTransaction=new org.springframework.transaction.support.TransactionTemplate(transactions);
  separateTransaction.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  org.mockito.Mockito.doAnswer(call -> {
   Object rows=call.callRealMethod();
   separateTransaction.executeWithoutResult(status -> db.jdbc.update("delete from project where id=?",id));
   return rows;
  }).when(db).rows(org.mockito.ArgumentMatchers.startsWith("select p.* from project p join project_member"),
      org.mockito.ArgumentMatchers.any(Object[].class));
  try {
   mvc.perform(get("/api/projects").param("search",name)).andExpect(status().isOk())
       .andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.items[0].id").value(id.toString()));
   assertEquals(0L,db.jdbc.queryForObject("select count(*) from project where id=?",Long.class,id));
  } finally {
   db.jdbc.update("delete from project where id=?",id);
  }
 }
 @Test @WithMockUser("catarina") void projectAndTaskResponsesMatchTheOpenApiFields()throws Exception{
  var schemas=json.readTree(java.nio.file.Path.of("openapi.yaml").toFile()).at("/components/schemas");
  var project=project();
  var task=(UUID)tasks.create(project,catarina(),"Contract task","Details").get("id");
  tasks.assign(project,catarina(),task,catarina());
  var taskPage=json.readTree(mvc.perform(get(base(project))).andExpect(status().isOk())
      .andReturn().getResponse().getContentAsString());
  var taskBody=taskPage.at("/items/0");
  assertFields(schemas.get("TaskPage").get("properties"),taskPage);
  assertFields(schemas.get("Task").get("properties"),taskBody);
  assertFields(schemas.get("User").get("properties"),taskBody.at("/assignees/0"));
  var projectBody=json.readTree(mvc.perform(get("/api/projects/"+project)).andExpect(status().isOk())
      .andReturn().getResponse().getContentAsString());
  var projectSchema=schemas.get("Project").get("properties");
  assertFields(projectSchema,projectBody);
  assertFields(projectSchema.at("/counts/properties"),projectBody.get("counts"));
 }

 @org.junit.jupiter.params.ParameterizedTest(name="{0} {1}")
 @org.junit.jupiter.params.provider.CsvSource({
     "GET, /", "PUT, /", "DELETE, /", "GET, /members", "POST, /members", "GET, /eligible-users",
     "GET, /tasks", "POST, /tasks", "GET, /tasks/{task}", "PUT, /tasks/{task}", "DELETE, /tasks/{task}",
     "POST, /tasks/{task}/assignees", "DELETE, /tasks/{task}/assignees/{user}",
     "GET, /tasks/{task}/comments", "POST, /tasks/{task}/comments"
 })
 void protectedRoutesRequireMembership(String method, String suffix) throws Exception {
  UUID p=project();
  UUID t=(UUID)tasks.create(p,catarina(),"Protected task","Details").get("id");
  UUID outsider=projects.user("taylor");
  var fullPath="/api/projects/"+p+(suffix.equals("/")?"":suffix.replace("{task}",t.toString())
      .replace("{user}",outsider.toString()));
  var request=request(org.springframework.http.HttpMethod.valueOf(method),fullPath).with(csrf())
      .param("version","0").contentType("application/json").content(body(Map.of(
        "name","Project name","title","Task title","description","Details","userId",outsider,
        "status","todo","priority","medium","content","Comment","version",0)));
  mvc.perform(request).andExpect(status().isUnauthorized());
  request.with(user("taylor"));
  mvc.perform(request).andExpect(status().isNotFound())
      .andExpect(jsonPath("$.message").value("Resource not found."));
  assertEquals("Protected task",db.jdbc.queryForObject("select title from task where id=?",String.class,t));
 }

 @org.junit.jupiter.params.ParameterizedTest(name="{0} {1}")
 @org.junit.jupiter.params.provider.CsvSource({"PUT, /", "DELETE, /", "GET, /eligible-users", "POST, /members"})
 @WithMockUser("sam")
 void projectManagementRequiresTheProjectManager(String method, String suffix) throws Exception {
  UUID p=project();
  projects.addMember(p,catarina(),projects.user("sam"));
  String path="/api/projects/"+p+(suffix.equals("/")?"":suffix);
  mvc.perform(request(org.springframework.http.HttpMethod.valueOf(method),path).with(csrf())
       .param("version","0").contentType("application/json")
       .content(body(Map.of("name","Changed","version",0,"userId",projects.user("taylor")))))
      .andExpect(status().isForbidden());
  assertEquals("Test project",db.jdbc.queryForObject("select name from project where id=?",String.class,p));
  assertEquals(2L,db.jdbc.queryForObject("select count(*) from project_member where project_id=?",Long.class,p));
 }

 @Test @WithMockUser("sam")
 void memberSeesProjectsFromDifferentManagersAndSearchTreatsWildcardsLiterally()throws Exception{
  UUID first=(UUID)projects.create(catarina(),"Test %_! launch","").get("id");
  UUID taylor=projects.user("taylor");
  UUID second=(UUID)projects.create(taylor,"Test second launch","").get("id");
  UUID privateProject=(UUID)projects.create(taylor,"Test private launch","").get("id");
  projects.addMember(first,catarina(),projects.user("sam"));
  projects.addMember(second,taylor,projects.user("sam"));

  var all=json.readTree(mvc.perform(get("/api/projects").param("search","  TeSt  "))
      .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(2))
      .andReturn().getResponse().getContentAsString());
  Set<String> ids=new HashSet<>();
  all.get("items").forEach(item->ids.add(item.get("id").asText()));
  assertEquals(Set.of(first.toString(),second.toString()),ids);
  mvc.perform(get("/api/projects/"+privateProject)).andExpect(status().isNotFound());
  mvc.perform(get("/api/projects").param("search","%_!").param("limit","1"))
      .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1))
      .andExpect(jsonPath("$.items[0].id").value(first.toString()))
      .andExpect(jsonPath("$.items[0].counts.percentage").value(0));
  mvc.perform(get("/api/projects").param("search","Test").param("limit","1").param("offset","2"))
      .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(2))
      .andExpect(jsonPath("$.items").isEmpty());
 }

 @Test @WithMockUser("catarina")
 void projectCrudValidatesVersionsAndCascadesMemberships()throws Exception{
  var created=mvc.perform(post("/api/projects").with(csrf()).contentType("application/json")
       .content(body(Map.of("name","  Manager project  "))))
      .andExpect(status().isCreated()).andExpect(jsonPath("$.name").value("Manager project"))
      .andExpect(jsonPath("$.description").value("")).andReturn();
  String path="/api/projects/"+json.readTree(created.getResponse().getContentAsString()).get("id").asText();
  mvc.perform(get(path+"/members")).andExpect(status().isOk())
      .andExpect(jsonPath("$[0].username").value("catarina"));
  mvc.perform(put(path).with(csrf()).contentType("application/json")
       .content(body(Map.of("name","Renamed","description"," Updated ","version",0))))
      .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1));
  mvc.perform(delete(path).with(csrf()).param("version","0")).andExpect(status().isConflict());
  mvc.perform(get(path)).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Renamed"))
      .andExpect(jsonPath("$.description").value("Updated"));
  mvc.perform(delete(path).with(csrf()).param("version","1")).andExpect(status().isNoContent());
  mvc.perform(get(path+"/members")).andExpect(status().isNotFound());
 }

 @Test @WithMockUser("sam")
 void memberCanReopenAndDeleteTaskAndAllItsCollaborationData()throws Exception{
  UUID p=project();
  UUID member=projects.user("sam");
  projects.addMember(p,catarina(),member);
  UUID t=(UUID)tasks.create(p,member,"Task","Details").get("id");
  tasks.assign(p,member,t,member);
  String path=base(p)+"/"+t;
  mvc.perform(post(path+"/comments").with(csrf()).contentType("application/json")
       .content(body(Map.of("content","  First comment  ","authorId",catarina()))))
      .andExpect(status().isCreated()).andExpect(jsonPath("$.authorId").value(member.toString()));
  mvc.perform(get(path+"/comments")).andExpect(status().isOk())
      .andExpect(jsonPath("$[0].content").value("First comment"))
      .andExpect(jsonPath("$[0].authorName").value("Sam Lee"));
  mvc.perform(put(path).with(csrf()).contentType("application/json")
       .content(body(Map.of("title","Task","description","Details","status","done","priority","low","version",0))))
      .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(1));
  mvc.perform(put(path).with(csrf()).contentType("application/json")
       .content(body(Map.of("title","Task","description","Details","status","in_progress","priority","high","version",1))))
      .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(2));
  mvc.perform(get("/api/projects/"+p)).andExpect(jsonPath("$.counts.done").value(0))
      .andExpect(jsonPath("$.counts.in_progress").value(1));
  mvc.perform(delete(path).with(csrf()).param("version","1")).andExpect(status().isConflict());
  mvc.perform(delete(path).with(csrf()).param("version","2")).andExpect(status().isNoContent());
  mvc.perform(get(path+"/comments")).andExpect(status().isNotFound());
  assertEquals(0L,db.jdbc.queryForObject("select count(*) from task_comment where task_id=?",Long.class,t));
  assertEquals(0L,db.jdbc.queryForObject("select count(*) from task_assignee where task_id=?",Long.class,t));
 }

 @Test @WithMockUser("catarina")
 void rejectsInvalidOptionsAndDoesNotPersistInvalidWrites()throws Exception{
  UUID p=project();
  UUID t=(UUID)tasks.create(p,catarina(),"Task","Details").get("id");
  String path=base(p)+"/"+t;
  mvc.perform(get(base(p)).param("status","closed")).andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.fields.status").exists());
  mvc.perform(get(base(p)).param("priority","urgent")).andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.fields.priority").exists());
  mvc.perform(get(base(p)).param("limit","101")).andExpect(status().isBadRequest());
  mvc.perform(get("/api/projects").param("offset","-1")).andExpect(status().isBadRequest());
  mvc.perform(post(path+"/comments").with(csrf()).contentType("application/json")
       .content(body(Map.of("content","x".repeat(2001))))).andExpect(status().isBadRequest());
  mvc.perform(put(path).with(csrf()).contentType("application/json")
       .content(body(Map.of("title","Task","description","Details","status","done","priority","medium"))))
      .andExpect(status().isBadRequest());
  mvc.perform(post(base(p)).with(csrf()).contentType("application/json")
       .content(body(Map.of("title","A\0B","description","Details"))))
      .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.title").exists());
  mvc.perform(get(path)).andExpect(jsonPath("$.version").value(0));
  assertEquals(0L,db.jdbc.queryForObject("select count(*) from task_comment where task_id=?",Long.class,t));
 }

 private static void assertFields(com.fasterxml.jackson.databind.JsonNode documented,com.fasterxml.jackson.databind.JsonNode actual){
  Set<String> expectedFields=new HashSet<>(), actualFields=new HashSet<>();
  documented.fieldNames().forEachRemaining(expectedFields::add);
  actual.fieldNames().forEachRemaining(actualFields::add);
  assertEquals(expectedFields,actualFields);
 }
 @Test void readinessChecksTheDatabaseWithoutAuthentication()throws Exception{
  mvc.perform(get("/ready")).andExpect(status().isNoContent());
 }
 @Test void authenticationCsrfAndLogout()throws Exception{
  mvc.perform(get("/api/projects")).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/auth/login").contentType("application/json").content(body(Map.of("username","catarina","password","MatrizDemo!2026")))).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CSRF_INVALID"));
  mvc.perform(post("/api/auth/login").with(csrf()).contentType("application/json").content(body(Map.of("username","catarina","password","wrong")))).andExpect(status().isUnauthorized());
  var result=mvc.perform(post("/api/auth/login").with(csrf()).contentType("application/json").content(body(Map.of("username","catarina","password","MatrizDemo!2026")))).andExpect(status().isOk()).andReturn();
  var session=(MockHttpSession)result.getRequest().getSession(false);
  mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.username").value("catarina"));
  mvc.perform(post("/api/auth/logout").session(session).with(csrf())).andExpect(status().isOk());assertTrue(session.isInvalid());
  mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
 }
}
