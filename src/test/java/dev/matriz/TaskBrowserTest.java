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
            page.getByLabel("Username").fill("catarina");
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
            search(page, name);
            page.context().clearCookies();
            page.evaluate("hash => location.hash = hash", "#/projects/" + project);
            assertThat(page.getByLabel("Username")).isVisible();
            enterCredentials(page);
            assertThat(heading(page, name)).isVisible();
            page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("All projects")).click();
            assertThat(page.getByLabel("Find a project")).hasValue("");
            search(page, name);
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign out")).click();
            assertThat(page.getByLabel("Username")).isVisible();
            page.getByLabel("Username").fill("sam");
            page.getByLabel("Password").fill("MatrizDemo!2026");
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign in to your workspace")).click();
            assertThat(page.locator("#identity")).containsText("Sam Lee");
            assertThat(heading(page, "Good work starts here.")).isVisible();
            assertThat(page.getByLabel("Find a project")).hasValue("");
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
            assertThat(heading(page, "Original title")).isVisible();
            String path = "**/api/projects/" + project + "/tasks/" + task + "/comments";
            var delayed = new java.util.concurrent.atomic.AtomicReference<Route>();
            page.route(path, route -> {
                if (route.request().method().equals("GET")) delayed.set(route);
                else route.resume();
            });
            var comment = page.getByLabel("Add a comment");
            comment.fill("A separate comment");
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Post comment")).click();
            page.waitForCondition(() -> delayed.get() != null);
            assertThat(comment).hasValue("");
            page.getByLabel("Title").fill("Draft title");
            delayed.get().fulfill(new Route.FulfillOptions().setStatus(500)
                .setContentType("application/json").setBody("{}"));
            var retry = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Retry refresh"));
            assertThat(retry).isVisible();
            page.unroute(path);
            retry.click();
            org.junit.jupiter.api.Assertions.assertEquals(1L, jdbc.queryForObject(
                "select count(*) from task_comment where task_id=?", Long.class, task));
            assertThat(page.getByText("A separate comment")).isVisible();
            assertThat(page.getByLabel("Title")).hasValue("Draft title");
            assertThat(heading(page, "Original title")).isVisible();
        } finally {
            jdbc.update("delete from project where id=?", project);
        }
    }


    @Test
    @org.junit.jupiter.api.Tag("sanity")
    void browserSanityChecksReadinessSignInTaskCreationAndSignOut() {
        String name = "Sanity project " + UUID.randomUUID();
        UUID project = seedProject(name);
        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            var readiness = page.request().get("http://127.0.0.1:" + port + "/ready");
            org.junit.jupiter.api.Assertions.assertEquals(204, readiness.status());
            signIn(page);

            page.evaluate("hash => location.hash = hash", "#/projects/" + project);
            assertThat(heading(page, name)).isVisible();
            String title = "Sanity check task";
            page.getByLabel("Title").fill(title);
            page.getByLabel("Description *", new Page.GetByLabelOptions().setExact(true)).fill("Saved using the browser");
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Create task")).click();
            assertThat(page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName(title))).isVisible();
            page.reload();
            assertThat(page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName(title))).isVisible();
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign out")).click();
            assertThat(page.getByLabel("Username")).isVisible();
            org.junit.jupiter.api.Assertions.assertEquals(401,
                page.request().get("http://127.0.0.1:" + port + "/api/auth/me").status());
        } finally {
            jdbc.update("delete from project where id=?", project);
        }
    }

    @Test
    void unicodeTitleLimitsAndFieldErrorsWorkInTheBrowser() {
        UUID project = seedProject("Validation project " + UUID.randomUUID());
        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch()) {
            Page page = browser.newPage();
            signIn(page);
            page.evaluate("hash => location.hash = hash", "#/projects/" + project);
            var title = page.getByLabel("Title");
            var create = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Create task"));
            title.fill("   ");
            page.getByLabel("Description *").fill("Valid description");
            create.click();
            assertThat(title).hasAttribute("aria-invalid", "true");
            assertThat(title).hasAttribute("aria-describedby", "title-error");
            assertThat(page.locator("#title-error")).containsText("120 characters");
            String boundary = "x".repeat(119) + "\uD83D\uDE42";
            title.fill(boundary + "x");
            assertThat(page.locator("#title-error")).hasCount(0);
            create.click();
            assertThat(page.locator("#title-error")).isVisible();
            title.fill(boundary);
            create.click();
            assertThat(page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName(boundary))).isVisible();
        } finally {
            jdbc.update("delete from project where id=?", project);
        }
    }

    @Test
    void staleDeletionRequiresReviewAndFreshConfirmation() {
        UUID project = seedProject("Deletion check " + UUID.randomUUID());
        UUID task = UUID.randomUUID();
        try (Playwright playwright = Playwright.create();
             Browser browser = playwright.chromium().launch()) {
            jdbc.update("insert into task(id,project_id,title,description) values(?,?,?,?)",
                task, project, "Disposable task", "Before edit");
            Page page = browser.newPage();
            signIn(page);
            page.evaluate("hash => location.hash = hash", "#/projects/" + project + "/tasks/" + task);
            var delete = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Delete task").setExact(true));
            var confirm = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Confirm deletion"));
            delete.click();
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Cancel")).click();
            assertThat(confirm).isHidden();
            jdbc.update("update task set description=?,version=version+1 where id=?", "Changed elsewhere", task);
            delete.click();
            confirm.click();
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Review latest task")).click();
            assertThat(page.getByLabel("Description *")).hasValue("Changed elsewhere");
            assertThat(confirm).isHidden();
            delete.click();
            confirm.click();
            assertThat(heading(page, "Tasks")).isVisible();
            page.reload();
            assertThat(heading(page, "Tasks")).isVisible();
            assertThat(page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Disposable task"))).hasCount(0);
            org.junit.jupiter.api.Assertions.assertEquals(0L, jdbc.queryForObject(
                "select count(*) from task where id=?", Long.class, task));
        } finally {
            jdbc.update("delete from project where id=?", project);
        }
    }

    private static void search(Page page, String name) {
        page.getByLabel("Find a project").fill(name);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Search").setExact(true)).click();
        assertThat(page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName(name))).isVisible();
    }

    private UUID seedProject(String name) {
        UUID id = UUID.randomUUID();
        UUID manager = jdbc.queryForObject("select id from app_user where username=?", UUID.class, "catarina");
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
        page.getByLabel("Username").fill("catarina");
        page.getByLabel("Password").fill("MatrizDemo!2026");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign in to your workspace")).click();
    }

    private static Locator heading(Page page, String name) {
        return page.getByRole(AriaRole.HEADING, new Page.GetByRoleOptions().setName(name).setExact(true));
    }
}
