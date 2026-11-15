package dev.matriz;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.UUID;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@EnabledIfEnvironmentVariable(named = "MATRIZ_E2E", matches = "true")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"matriz.demo.enabled=true", "server.servlet.session.cookie.secure=false"})
class TaskBrowserTest {
    @org.springframework.beans.factory.annotation.Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    @LocalServerPort int port;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> System.getenv().getOrDefault(
            "MATRIZ_TEST_DB_URL", "jdbc:postgresql://127.0.0.1:5432/matriz_test"));
    }

    @Test
    void signedInMemberCreatesTaskAndFindsItInTheList() {
        String suffix = UUID.randomUUID().toString();
        String project = "QA project " + suffix;
        String title = "QA task " + suffix;
        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            page.navigate("http://127.0.0.1:" + port);
            page.getByLabel("Username").fill("alex");
            page.getByLabel("Password").fill("MatrizDemo!2026");
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign in to your workspace")).click();
            page.getByLabel("Project name").fill(project);
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Create project")).click();
            assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName(project))).isVisible();

            page.getByLabel("Title").fill(title);
            page.getByLabel("Description *", new Page.GetByLabelOptions().setExact(true)).fill("Created through the browser.");
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Create task")).click();
            assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName(title))).isVisible();
            page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName(project)).click();
            assertThat(page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName(title))).isVisible();
        } finally {
            jdbc.update("delete from project where name = ?", project);
        }
    }
}
