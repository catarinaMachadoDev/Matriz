package dev.matriz.identity;

import dev.matriz.shared.Data;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

import org.springframework.context.annotation.*;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.*;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService users(Data db) {
        return username -> {
            var rows = db.rows("select username,password_hash from app_user where username=?", username);
            if (rows.isEmpty()) throw new UsernameNotFoundException("Invalid credentials");
            var row = rows.getFirst();
            return User.withUsername((String) row.get("username")).password((String) row.get("passwordHash")).roles("USER").build();
        };
    }

    @Bean
    AuthenticationManager authenticationManager(UserDetailsService users, PasswordEncoder encoder) {
        var p = new DaoAuthenticationProvider(users);
        p.setPasswordEncoder(encoder);
        return new ProviderManager(p);
    }

    @Bean
    HttpSessionCsrfTokenRepository csrfRepository() {
        var r = new HttpSessionCsrfTokenRepository();
        r.setHeaderName("X-CSRF-Token");
        return r;
    }

    @Bean
    SecurityFilterChain filter(HttpSecurity http, HttpSessionCsrfTokenRepository csrf, ObjectMapper mapper) throws Exception {
        return http.authorizeHttpRequests(a -> a.requestMatchers("/", "/index.html", "/ready", "/app.js", "/styles.css", "/favicon.ico", "/error", "/api/auth/login", "/api/auth/csrf").permitAll().anyRequest().authenticated())
                .csrf(c -> c.csrfTokenRepository(csrf).csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                .exceptionHandling(e -> e.authenticationEntryPoint((req, res, ex) -> {
                            res.setStatus(401);
                            res.setContentType("application/json");
                            mapper.writeValue(res.getOutputStream(), Map.of("code", "UNAUTHENTICATED", "message", "Please sign in."));
                        })
                        .accessDeniedHandler((req, res, ex) -> {
                            res.setStatus(403);
                            res.setContentType("application/json");
                            boolean invalidCsrf = ex instanceof CsrfException;
                            mapper.writeValue(res.getOutputStream(), Map.of(
                                    "code", invalidCsrf ? "CSRF_INVALID" : "FORBIDDEN",
                                    "message", invalidCsrf ? "Refresh your session and try again." : "Request denied."));
                        }))
                .headers(h -> h.contentSecurityPolicy(c -> c.policyDirectives("default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; frame-ancestors 'none'; base-uri 'self'")))
                .requestCache(c -> c.disable()).formLogin(c -> c.disable()).httpBasic(c -> c.disable()).logout(c -> c.disable()).build();
    }
}
