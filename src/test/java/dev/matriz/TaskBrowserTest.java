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

            UUID projectId = jdbc.queryForObject("select id from project where name=?", UUID.class, project);
            jdbc.update("""
                insert into task(id,project_id,title,description,status,created_at)
                select gen_random_uuid(), ?, 'Earlier task ' || n, 'Details', 'done',
                       current_timestamp - interval '1 day'
                from generate_series(1,21) n
                """, projectId);
            page.getByLabel("Status", new Page.GetByLabelOptions().setExact(true)).selectOption("done");
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Apply filters")).click();
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Next")).click();
            assertThat(page.getByText("21–21 of 21")).isVisible();

            page.getByLabel("Title").fill(title);
            page.getByLabel("Description *", new Page.GetByLabelOptions().setExact(true)).fill("Created through the browser.");
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Create task")).click();
            assertThat(page.getByText("Task created.")).isVisible();
            assertThat(page.getByLabel("Status", new Page.GetByLabelOptions().setExact(true))).hasValue("");
            assertThat(page.locator(".task-row a").first()).hasText(title);
            page.reload();
            assertThat(page.locator(".task-row a").first()).hasText(title);
            page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName(title).setExact(true)).click();
            assertThat(heading(page, title)).isVisible();
        } finally {
            jdbc.update("delete from project where name = ?", project);
        }
    }
    @Test
    void delayedProjectResponseCannotReplaceTheCurrentProject() {
        String alphaName = "Async Alpha " + UUID.randomUUID();
        String betaName = "Async Beta " + UUID.randomUUID();
        UUID alpha = seedProject(alphaName);
        UUID beta = seedProject(betaName);
        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            signIn(page);
            var delayed = new java.util.concurrent.atomic.AtomicReference<Route>();
            page.route("**/api/projects/" + alpha + "/eligible-users", delayed::set);
            page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName(java.util.regex.Pattern.compile(alphaName))).click();
            page.waitForCondition(() -> delayed.get() != null);
            page.evaluate("hash => location.hash = hash", "#/projects/" + beta);
            assertThat(heading(page, betaName)).isVisible();
            page.waitForResponse(response -> response.url().endsWith("/" + alpha + "/eligible-users"),
                () -> delayed.get().fulfill(new Route.FulfillOptions().setStatus(200).setContentType("application/json").setBody("[]")));
            page.evaluate("() => new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve)))");
            assertThat(heading(page, betaName)).isVisible();
            page.getByLabel("Title").fill("Task for Beta");
            page.getByLabel("Description *", new Page.GetByLabelOptions().setExact(true)).fill("Beta details");
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Create task")).click();
            assertThat(page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Task for Beta").setExact(true))).isVisible();
            org.junit.jupiter.api.Assertions.assertEquals(beta, jdbc.queryForObject(
                "select project_id from task where project_id in (?,?) and title=?", UUID.class, alpha, beta, "Task for Beta"));
        } finally {
            jdbc.update("delete from project where id in (?,?)", alpha, beta);
        }
    }

    @Test
    void canSignBackInAfterTheSessionCookieExpires() {
        String name = "Session project " + UUID.randomUUID();
        UUID project = seedProject(name);
        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            signIn(page);
            page.context().clearCookies();
            page.evaluate("hash => location.hash = hash", "#/projects/" + project);
            assertThat(page.getByLabel("Username")).isVisible();
            enterCredentials(page);
            assertThat(heading(page, name)).isVisible();

            page.context().clearCookies();
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign out")).click();
            assertThat(page.getByLabel("Username")).isVisible();
            page.getByLabel("Username").fill("sam");
            page.getByLabel("Password").fill("MatrizDemo!2026");
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign in to your workspace")).click();
            assertThat(page.locator("#identity")).containsText("Sam Lee");
            assertThat(heading(page, "Good work starts here.")).isVisible();
        } finally {
            jdbc.update("delete from project where id=?", project);
        }
    }

    @Test
    void postingACommentPreservesTheUnsavedTaskEdit() {
        UUID project = seedProject("Draft project " + UUID.randomUUID());
        UUID task = UUID.randomUUID();
        jdbc.update("insert into task(id,project_id,title,description) values(?,?,?,?)", task, project, "Original title", "Details");
        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            signIn(page);
            page.evaluate("hash => location.hash = hash", "#/projects/" + project + "/tasks/" + task);
            page.getByLabel("Title").fill("Draft title");
            page.getByLabel("Add a comment").fill("A separate comment");
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Post comment")).click();
            assertThat(page.getByText("A separate comment")).isVisible();
            assertThat(page.getByLabel("Title")).hasValue("Draft title");
            assertThat(heading(page, "Original title")).isVisible();
        } finally {
            jdbc.update("delete from project where id=?", project);
        }
    }

    private UUID seedProject(String name) {
        UUID id = UUID.randomUUID();
        UUID manager = jdbc.queryForObject("select id from app_user where username=?", UUID.class, "alex");
        jdbc.update("insert into project(id,name,manager_id) values(?,?,?)", id, name, manager);
        jdbc.update("insert into project_member(project_id,user_id) values(?,?)", id, manager);
        return id;
    }

    private void signIn(Page page) {
        page.navigate("http://127.0.0.1:" + port);
        enterCredentials(page);
        assertThat(heading(page, "Good work starts here.")).isVisible();
    }

    private static void enterCredentials(Page page) {
        page.getByLabel("Username").fill("alex");
        page.getByLabel("Password").fill("MatrizDemo!2026");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign in to your workspace")).click();
    }

    private static Locator heading(Page page, String name) {
        return page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName(name).setExact(true));
    }
}
