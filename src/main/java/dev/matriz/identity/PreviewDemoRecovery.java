package dev.matriz.identity;

import dev.matriz.shared.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "matriz.preview.catarina-reset-password")
public class PreviewDemoRecovery implements ApplicationRunner {
    private static final int CATARINA_RESET_MARKER = 2;
    private final Data db;
    private final PasswordEncoder encoder;
    private final String password;

    public PreviewDemoRecovery(
            Data db, PasswordEncoder encoder,
            @Value("${matriz.preview.catarina-reset-password}") String password) {
        this.db = db;
        this.encoder = encoder;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int length = password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
        if (length < 12 || length > 72 || password.isBlank()) {
            throw new IllegalStateException("Preview recovery requires a password of 12 to 72 UTF-8 bytes.");
        }
        db.jdbc.execute("create table if not exists demo_seed_marker (id integer primary key)");
        if (db.jdbc.update("insert into demo_seed_marker values (?) on conflict do nothing", CATARINA_RESET_MARKER) == 0) {
            return;
        }
        int updated = db.jdbc.update("update app_user set password_hash=? where username='catarina'",
                encoder.encode(password));
        if (updated != 1) {
            throw new IllegalStateException("The Catarina demo account does not exist.");
        }
    }
}
