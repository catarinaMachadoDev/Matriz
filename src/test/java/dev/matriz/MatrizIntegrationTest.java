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
 @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired ProjectService projects; @Autowired TaskService tasks; @Autowired Data db;
 UUID alex(){return projects.user("alex");}
 UUID project(){return (UUID)projects.create(alex(),"Test project","Verification").get("id");}
 String base(UUID p){return "/api/projects/"+p+"/tasks";}
 String body(Object v)throws Exception{return json.writeValueAsString(v);}

 @Test @WithMockUser("alex") void mandatoryDescriptionAndWhitespaceRejected()throws Exception{
  var p=project();for(String description:List.of("","   ","x".repeat(2001)))mvc.perform(post(base(p)).with(csrf()).contentType("application/json").content(body(Map.of("title","Valid","description",description)))).andExpect(status().isBadRequest());
  mvc.perform(post(base(p)).with(csrf()).contentType("application/json").content(body(Map.of("title","   ","description","Valid")))).andExpect(status().isBadRequest());
  assertEquals(0L,db.jdbc.queryForObject("select count(*) from task where project_id=?",Long.class,p));
 }
 @Test @WithMockUser("taylor") void directCrossProjectAccessIsHidden()throws Exception{
  var p=project();var t=(UUID)tasks.create(p,alex(),"Private","Protected content").get("id");
  mvc.perform(get(base(p)+"/"+t)).andExpect(status().isNotFound());
  mvc.perform(post(base(p)).with(csrf()).contentType("application/json").content(body(Map.of("title","Intrusion","description","Denied")))).andExpect(status().isNotFound());
  mvc.perform(delete("/api/projects/"+p).param("version","0").with(csrf())).andExpect(status().isNotFound());
 }
 @Test @WithMockUser("alex") void createThenReadUsesRealPostgres()throws Exception{
  var p=project();var result=mvc.perform(post(base(p)).with(csrf()).contentType("application/json").content(body(Map.of("title","  Persistent task  ","description","  Required details  ")))).andExpect(status().isCreated()).andExpect(jsonPath("$.title").value("Persistent task")).andReturn();
  String id=json.readTree(result.getResponse().getContentAsString()).get("id").asText();
  assertEquals("Required details",db.jdbc.queryForObject("select description from task where id=?",String.class,UUID.fromString(id)));
  mvc.perform(get(base(p)+"/"+id)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("todo")).andExpect(jsonPath("$.priority").value("medium")).andExpect(jsonPath("$.assignees").isEmpty());
 }
 @Test @WithMockUser("alex") void staleTaskEditReturnsConflict()throws Exception{
  var p=project();var t=(UUID)tasks.create(p,alex(),"Task","Details").get("id");var b=Map.of("title","Updated","description","Updated details","status","done","priority","high","version",0);
  mvc.perform(put(base(p)+"/"+t).with(csrf()).contentType("application/json").content(body(b))).andExpect(status().isOk());
  mvc.perform(put(base(p)+"/"+t).with(csrf()).contentType("application/json").content(body(b))).andExpect(status().isConflict());
 }
 @Test @WithMockUser("alex") void multipleAssigneesDeduplicateAndRequireMembership()throws Exception{
  var p=project();var sam=projects.user("sam");projects.addMember(p,alex(),sam);var t=(UUID)tasks.create(p,alex(),"Task","Details").get("id");
  for(var u:List.of(alex(),sam,sam))mvc.perform(post(base(p)+"/"+t+"/assignees").with(csrf()).contentType("application/json").content(body(Map.of("userId",u)))).andExpect(status().isNoContent());
  mvc.perform(get(base(p)+"/"+t)).andExpect(jsonPath("$.assignees.length()").value(2));
  mvc.perform(post(base(p)+"/"+t+"/assignees").with(csrf()).contentType("application/json").content(body(Map.of("userId",projects.user("taylor"))))).andExpect(status().isBadRequest());
  mvc.perform(delete(base(p)+"/"+t+"/assignees/"+sam).with(csrf())).andExpect(status().isNoContent());
  mvc.perform(get(base(p)+"/"+t)).andExpect(jsonPath("$.assignees.length()").value(1));
 }
 @Test @WithMockUser("alex") void commentAuthorAndCascadeDeletion()throws Exception{
  var p=project();var t=(UUID)tasks.create(p,alex(),"Task","Details").get("id");tasks.assign(p,alex(),t,alex());
  mvc.perform(post(base(p)+"/"+t+"/comments").with(csrf()).contentType("application/json").content(body(Map.of("content","Useful comment")))).andExpect(status().isCreated()).andExpect(jsonPath("$.authorId").value(alex().toString())).andExpect(jsonPath("$.createdAt").exists());
  mvc.perform(delete("/api/projects/"+p).param("version","0").with(csrf())).andExpect(status().isNoContent());
  for(String table:List.of("task_comment","task_assignee"))assertEquals(0L,db.jdbc.queryForObject("select count(*) from "+table+" where task_id=?",Long.class,t));
 }
 @Test @WithMockUser("sam") void memberCannotManageProjectButCanEditTasks()throws Exception{
  var p=project();projects.addMember(p,alex(),projects.user("sam"));
  mvc.perform(put("/api/projects/"+p).with(csrf()).contentType("application/json").content(body(Map.of("name","Forbidden","description","","version",0)))).andExpect(status().isForbidden());
  mvc.perform(post(base(p)).with(csrf()).contentType("application/json").content(body(Map.of("title","Member task","description","Allowed")))).andExpect(status().isCreated());
 }
 @Test @WithMockUser("alex") void filtersAndCountsCoverAllPages()throws Exception{
  var p=project();for(int i=0;i<3;i++){var t=(UUID)tasks.create(p,alex(),"Task "+i,"Details").get("id");if(i<2)tasks.update(p,alex(),t,"Task "+i,"Details","done","high",0L);}
  mvc.perform(get(base(p)).param("status","done").param("priority","high").param("limit","1")).andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.total").value(2));
  mvc.perform(get("/api/projects/"+p)).andExpect(jsonPath("$.counts.total").value(3)).andExpect(jsonPath("$.counts.done").value(2)).andExpect(jsonPath("$.counts.percentage").value(66.7));
 }
 @Test void authenticationCsrfAndLogout()throws Exception{
  mvc.perform(get("/api/projects")).andExpect(status().isUnauthorized());
  mvc.perform(post("/api/auth/login").contentType("application/json").content(body(Map.of("username","alex","password","MatrizDemo!2026")))).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CSRF_INVALID"));
  mvc.perform(post("/api/auth/login").with(csrf()).contentType("application/json").content(body(Map.of("username","alex","password","wrong")))).andExpect(status().isUnauthorized());
  var result=mvc.perform(post("/api/auth/login").with(csrf()).contentType("application/json").content(body(Map.of("username","alex","password","MatrizDemo!2026")))).andExpect(status().isOk()).andReturn();
  var session=(MockHttpSession)result.getRequest().getSession(false);
  mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.username").value("alex"));
  mvc.perform(post("/api/auth/logout").session(session).with(csrf())).andExpect(status().isOk());assertTrue(session.isInvalid());
  mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
 }
}
