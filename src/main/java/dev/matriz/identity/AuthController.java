package dev.matriz.identity;
import dev.matriz.shared.Data;
import static dev.matriz.shared.ApiError.Failure;
import java.util.*;
import jakarta.servlet.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.*;
import org.springframework.security.core.context.*;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.*;
@RestController @RequestMapping("/api/auth")
public class AuthController {
 private final AuthenticationManager manager;private final Data db;private final HttpSessionCsrfTokenRepository csrf;
 public AuthController(AuthenticationManager m,Data db,HttpSessionCsrfTokenRepository csrf){manager=m;this.db=db;this.csrf=csrf;}
 public record Login(String username,String password){}
 @GetMapping("/csrf") public Map<String,String> csrf(CsrfToken token){return Map.of("token",token.getToken(),"headerName",token.getHeaderName());}
 @PostMapping("/login") public Map<String,Object> login(@RequestBody Login body,HttpServletRequest req,HttpServletResponse res){
  if(body.username()==null||body.password()==null||body.username().length()>80||body.password().length()>200)throw new Failure(401,"Invalid username or password.");
  Authentication auth;try{auth=manager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(body.username(),body.password()));}catch(AuthenticationException e){throw new Failure(401,"Invalid username or password.");}
  req.getSession(true);req.changeSessionId();var context=SecurityContextHolder.createEmptyContext();context.setAuthentication(auth);SecurityContextHolder.setContext(context);new HttpSessionSecurityContextRepository().saveContext(context,req,res);csrf.saveToken(null,req,res);return me(auth);
 }
 @GetMapping("/me") public Map<String,Object> me(Authentication auth){return db.one("select id,username,display_name,can_create_projects from app_user where username=?",auth.getName());}
 @PostMapping("/logout") public Map<String,String> logout(HttpServletRequest req){var session=req.getSession(false);if(session!=null)session.invalidate();SecurityContextHolder.clearContext();return Map.of("message","Signed out.");}
}
