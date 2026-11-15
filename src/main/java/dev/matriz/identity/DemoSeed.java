package dev.matriz.identity;
import java.util.UUID;
import dev.matriz.shared.Data;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
@Component @ConditionalOnProperty(name="matriz.demo.enabled",havingValue="true")
public class DemoSeed implements ApplicationRunner {
 private final Data db;private final PasswordEncoder encoder;
 public DemoSeed(Data db,PasswordEncoder encoder){this.db=db;this.encoder=encoder;}
 @Transactional public void run(ApplicationArguments args){
  // Seed once only. Restart never recreates deleted demo records or resets passwords.
  db.jdbc.execute("create table if not exists demo_seed_marker (id integer primary key)");
  if(db.jdbc.update("insert into demo_seed_marker values(1) on conflict do nothing")==0)return;
  UUID alex=UUID.randomUUID(),sam=UUID.randomUUID(),taylor=UUID.randomUUID();
  for(Object[] u:new Object[][]{{alex,"alex","Alex Morgan"},{sam,"sam","Sam Lee"},{taylor,"taylor","Taylor Reed"}})db.jdbc.update("insert into app_user values(?,?,?,?)",u[0],u[1],u[2],encoder.encode("MatrizDemo!2026"));
  UUID shared=UUID.randomUUID(),privateProject=UUID.randomUUID();
  db.jdbc.update("insert into project(id,name,description,manager_id) values(?,?,?,?)",shared,"Website launch","Coordinate the fictional website launch.",alex);
  db.jdbc.update("insert into project_member values(?,?),(?,?)",shared,alex,shared,sam);
  db.jdbc.update("insert into project(id,name,description,manager_id) values(?,?,?,?)",privateProject,"Private planning","Only Taylor can access this demo project.",taylor);
  db.jdbc.update("insert into project_member values(?,?)",privateProject,taylor);
  String[] titles={"Review project scope","Draft homepage copy","Review wireframe","Prepare launch checklist"};String[] states={"done","done","in_progress","todo"};
  for(int i=0;i<titles.length;i++)db.jdbc.update("insert into task(id,project_id,title,description,status) values(?,?,?,?,?)",UUID.randomUUID(),shared,titles[i],"Complete and review this demonstration activity.",states[i]);
 }
}
