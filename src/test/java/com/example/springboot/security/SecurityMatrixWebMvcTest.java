package com.example.springboot.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.example.springboot.config.SecurityConfig;
import com.example.springboot.exception.GlobalExceptionHandler;
import com.example.springboot.repository.BatchRepository;
import com.example.springboot.repository.CourseRepository;
import com.example.springboot.repository.SectionRepository;
import com.example.springboot.repository.StudentRecordRepository;
import com.example.springboot.repository.SubjectRepository;
import com.example.springboot.repository.UserRepository;
import com.example.springboot.repository.UserSecurityAnswerRepository;
import com.example.springboot.service.AccountService;
import com.example.springboot.service.AdminService;
import com.example.springboot.service.ClassManagementService;
import com.example.springboot.service.CustomUserDetailsService;
import com.example.springboot.service.DocumentExportService;
import com.example.springboot.service.DocumentFolderService;
import com.example.springboot.service.DocumentGenerationService;
import com.example.springboot.service.DocumentService;
import com.example.springboot.service.RegistrarService;
import com.example.springboot.service.SecurityQuestionService;
import com.example.springboot.service.SessionAuthenticationHelper;
import com.example.springboot.service.SoChecklistService;
import com.example.springboot.service.StudentDetailsService;
import com.example.springboot.service.StudentNumberExportService;
import com.example.springboot.service.StudentNumberImportService;
import com.example.springboot.service.SystemLogExportService;
import com.example.springboot.service.SystemLogService;
import com.example.springboot.service.TrainerGradeService;
import com.example.springboot.service.TrainerService;
import com.example.springboot.support.RouteSweep;
import com.example.springboot.support.RouteSweep.Route;

/**
 * Authorization matrix: every registered /api/** route is exercised as anonymous and as each role,
 * and the outcome is compared with an independent oracle ({@link #expectedAccess(String)}) written
 * from the role rules in CLAUDE.md, not copied from SecurityConfig.
 * <p>
 * ISO 25010: Security
 */
@WebMvcTest
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
class SecurityMatrixWebMvcTest {

    /** Who may call a route, according to the oracle. */
    enum Access { PUBLIC, ADMIN, REGISTRAR, TRAINER, ACCOUNT, SETUP, VERIFY, RESET, ANY_LOGIN }

    private static final List<String> REAL_ROLES = List.of("ADMIN", "REGISTRAR", "TRAINER");
    private static final List<String> PENDING_ROLES =
            List.of("PENDING_SETUP", "PENDING_VERIFICATION", "PENDING_RESET");
    private static final List<String> ALL_ROLES = concat(REAL_ROLES, PENDING_ROLES);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @MockitoBean private CustomUserDetailsService customUserDetailsService;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private BatchRepository batchRepository;
    @MockitoBean private CourseRepository courseRepository;
    @MockitoBean private SectionRepository sectionRepository;
    @MockitoBean private StudentRecordRepository studentRecordRepository;
    @MockitoBean private SubjectRepository subjectRepository;
    @MockitoBean private UserSecurityAnswerRepository userSecurityAnswerRepository;
    @MockitoBean private AccountService accountService;
    @MockitoBean private AdminService adminService;
    @MockitoBean private ClassManagementService classManagementService;
    @MockitoBean private DocumentExportService documentExportService;
    @MockitoBean private DocumentFolderService documentFolderService;
    @MockitoBean private DocumentGenerationService documentGenerationService;
    @MockitoBean private DocumentService documentService;
    @MockitoBean private RegistrarService registrarService;
    @MockitoBean private SecurityQuestionService securityQuestionService;
    @MockitoBean private SessionAuthenticationHelper sessionAuthenticationHelper;
    @MockitoBean private SoChecklistService soChecklistService;
    @MockitoBean private StudentDetailsService studentDetailsService;
    @MockitoBean private StudentNumberExportService studentNumberExportService;
    @MockitoBean private StudentNumberImportService studentNumberImportService;
    @MockitoBean private SystemLogExportService systemLogExportService;
    @MockitoBean private SystemLogService systemLogService;
    @MockitoBean private TrainerGradeService trainerGradeService;
    @MockitoBean private TrainerService trainerService;

    // ===================== Independent oracle =====================

    /**
     * Who should be able to reach an /api path. Returns null when the path is not classified, which
     * T1-13 reports so that new routes are forced into this table.
     */
    static Access expectedAccess(String path) {
        if (path.equals("/api/auth") || path.startsWith("/api/auth/")) {
            return Access.PUBLIC; // login, logout, me: login must work anonymously
        }
        if (path.startsWith("/api/student-portal/") || path.startsWith("/api/student/")) {
            return Access.PUBLIC;
        }
        if (path.equals("/api/password-recovery/lookup")) {
            return Access.PUBLIC;
        }
        if (path.equals("/api/password-recovery/verify")) {
            return Access.VERIFY;
        }
        if (path.equals("/api/password-recovery/reset")) {
            return Access.RESET;
        }
        if (path.equals("/api/account/security-questions/setup")
                || path.equals("/api/account/security-questions/default-questions")) {
            return Access.SETUP;
        }
        if (path.startsWith("/api/account/")) {
            return Access.ACCOUNT;
        }
        if (path.startsWith("/api/admin/") || path.equals("/api/logs") || path.startsWith("/api/logs/")) {
            return Access.ADMIN;
        }
        if (path.startsWith("/api/registrar/")) {
            return Access.REGISTRAR;
        }
        if (path.startsWith("/api/trainer/")) {
            return Access.TRAINER;
        }
        if (path.startsWith("/api/lookup/")) {
            return Access.ANY_LOGIN; // shared dropdown data for logged-in staff
        }
        return null;
    }

    /** Roles (among ALL_ROLES) that the oracle says may pass. Anonymous is allowed only for PUBLIC. */
    static List<String> allowedRoles(Access access) {
        return switch (access) {
            case PUBLIC, ANY_LOGIN -> ALL_ROLES;
            case ADMIN -> List.of("ADMIN");
            case REGISTRAR -> List.of("REGISTRAR");
            case TRAINER -> List.of("TRAINER");
            case ACCOUNT -> REAL_ROLES;
            case SETUP -> List.of("PENDING_SETUP", "ADMIN", "REGISTRAR", "TRAINER");
            case VERIFY -> List.of("PENDING_VERIFICATION");
            case RESET -> List.of("PENDING_RESET");
        };
    }

    // ===================== helpers =====================

    private static List<String> concat(List<String> a, List<String> b) {
        List<String> r = new ArrayList<>(a);
        r.addAll(b);
        return List.copyOf(r);
    }

    private static RequestPostProcessor as(String role) {
        return user("tester").roles(role);
    }

    private int statusOf(HttpMethod method, String path, RequestPostProcessor auth) throws Exception {
        var builder = request(method, path);
        if (auth != null) {
            builder = builder.with(auth);
        }
        return mockMvc.perform(builder).andReturn().getResponse().getStatus();
    }

    private static boolean blocked(int status) {
        return status == 401 || status == 403;
    }

    private List<Route> routes() {
        return RouteSweep.allApiRoutes(handlerMapping);
    }

    private static HttpMethod httpMethod(Route r) {
        return HttpMethod.valueOf(r.method().name());
    }

    private static String where(Route r) {
        return r.method() + " " + r.pattern();
    }

    // ===================== tests =====================

    @Test
    void anonymousIsRejectedOnEveryProtectedApiRoute() throws Exception {
        List<String> problems = new ArrayList<>();
        int checked = 0;
        for (Route r : routes()) {
            Access access = expectedAccess(r.pattern());
            if (access == null || access == Access.PUBLIC) {
                continue;
            }
            checked++;
            MvcResult res = mockMvc.perform(request(httpMethod(r), RouteSweep.fill(r.pattern()))).andReturn();
            String contentType = res.getResponse().getContentType();
            if (res.getResponse().getStatus() != 401
                    || contentType == null || !contentType.startsWith(MediaType.APPLICATION_JSON_VALUE)) {
                problems.add(where(r) + " -> " + res.getResponse().getStatus() + " " + contentType);
            }
        }
        assertThat(checked).isGreaterThan(0);
        assertThat(problems).as("anonymous must get 401 JSON").isEmpty();
    }

    @Test
    void wrongRoleIsForbiddenOnEveryRoleGuardedRoute() throws Exception {
        List<String> problems = new ArrayList<>();
        int checked = 0;
        for (Route r : routes()) {
            Access access = expectedAccess(r.pattern());
            if (access != Access.ADMIN && access != Access.REGISTRAR && access != Access.TRAINER) {
                continue;
            }
            for (String role : ALL_ROLES) {
                if (allowedRoles(access).contains(role)) {
                    continue;
                }
                checked++;
                int status = statusOf(httpMethod(r), RouteSweep.fill(r.pattern()), as(role));
                if (status != 403) {
                    problems.add(role + " " + where(r) + " -> " + status);
                }
            }
        }
        assertThat(checked).isGreaterThan(0);
        assertThat(problems).as("other roles must get 403").isEmpty();
    }

    @Test
    void owningRoleIsNotBlockedOnEveryRoleGuardedRoute() throws Exception {
        List<String> problems = new ArrayList<>();
        int checked = 0;
        for (Route r : routes()) {
            Access access = expectedAccess(r.pattern());
            if (access != Access.ADMIN && access != Access.REGISTRAR && access != Access.TRAINER) {
                continue;
            }
            checked++;
            int status = statusOf(httpMethod(r), RouteSweep.fill(r.pattern()), as(access.name()));
            if (blocked(status)) {
                problems.add(access + " " + where(r) + " -> " + status);
            }
        }
        assertThat(checked).isGreaterThan(0);
        assertThat(problems).as("owning role must not be blocked").isEmpty();
    }

    @Test
    void publicRoutesAreReachableAnonymously() throws Exception {
        List<String> problems = new ArrayList<>();
        int checked = 0;
        for (Route r : routes()) {
            if (expectedAccess(r.pattern()) != Access.PUBLIC) {
                continue;
            }
            if (r.pattern().equals("/api/auth/me")) {
                continue; // the filter chain lets it through; the controller itself answers 401 without a session
            }
            checked++;
            int status = statusOf(httpMethod(r), RouteSweep.fill(r.pattern()), null);
            if (blocked(status)) {
                problems.add(where(r) + " -> " + status);
            }
        }
        assertThat(checked).isGreaterThan(0);
        assertThat(problems).as("public routes must not demand login").isEmpty();
        // The named entry points from the role rules must exist as public routes.
        assertThat(routes().stream().map(Route::pattern))
                .contains("/api/auth/login", "/api/password-recovery/lookup", "/api/student-portal/check-duplicate");
    }

    @Test
    void htmlPagesAreRoleGuarded() throws Exception {
        record Page(String path, String owner, String dashboard) {}
        List<Page> pages = new ArrayList<>();
        for (String p : List.of("/admin.html", "/logs.html", "/edit-user.html", "/add-user.html")) {
            pages.add(new Page(p, "ADMIN", "/admin.html"));
        }
        for (String p : List.of("/registrar.html", "/subjects.html", "/student-records.html", "/classes.html",
                "/sections.html", "/documents.html", "/generate-document.html", "/student-numbers.html")) {
            pages.add(new Page(p, "REGISTRAR", "/registrar.html"));
        }
        for (String p : List.of("/trainer.html", "/trainer-subjects.html", "/trainer-classes.html")) {
            pages.add(new Page(p, "TRAINER", "/trainer.html"));
        }
        List<String> dashboards = List.of("/admin.html", "/registrar.html", "/trainer.html");
        List<String> problems = new ArrayList<>();
        for (Page page : pages) {
            var anon = mockMvc.perform(get(page.path())).andReturn().getResponse();
            if (anon.getStatus() != 302 || !"/index.html".equals(anon.getRedirectedUrl())) {
                problems.add("anonymous " + page.path() + " -> " + anon.getStatus() + " " + anon.getRedirectedUrl());
            }
            for (String role : REAL_ROLES) {
                var res = mockMvc.perform(get(page.path()).with(as(role))).andReturn().getResponse();
                if (role.equals(page.owner())) {
                    if (res.getStatus() / 100 == 3) {
                        problems.add(role + " owner redirected from " + page.path() + " -> " + res.getRedirectedUrl());
                    }
                } else {
                    String expectedDashboard = dashboards.get(REAL_ROLES.indexOf(role));
                    if (res.getStatus() != 302 || !expectedDashboard.equals(res.getRedirectedUrl())) {
                        problems.add(role + " " + page.path() + " -> " + res.getStatus() + " "
                                + res.getRedirectedUrl() + " (expected " + expectedDashboard + ")");
                    }
                }
            }
        }
        assertThat(problems).isEmpty();
    }

    @Test
    void staticAssetsArePublic() throws Exception {
        for (String path : List.of("/css/x", "/js/x", "/images/x", "/index.html")) {
            assertThat(blocked(statusOf(HttpMethod.GET, path, null))).as(path).isFalse();
        }
    }

    @Test
    void accountApiNeedsARealRole() throws Exception {
        for (String role : REAL_ROLES) {
            assertThat(blocked(statusOf(HttpMethod.PUT, "/api/account/profile", as(role)))).as(role).isFalse();
        }
        assertThat(statusOf(HttpMethod.PUT, "/api/account/profile", null)).isEqualTo(401);
    }

    @Test
    void accountApiRejectsPendingSessions() throws Exception {
        for (String role : PENDING_ROLES) {
            assertThat(statusOf(HttpMethod.PUT, "/api/account/profile", as(role))).as(role).isEqualTo(403);
        }
    }

    /**
     * FINDING: /api/lookup/** has no explicit rule in SecurityConfig and falls through to
     * {@code anyRequest().authenticated()}. Half-authenticated PENDING_* sessions count as
     * authenticated, so they can read the lookup data. The secure/expected behaviour is 403 (as for
     * /api/account/**). This test pins the CURRENT behaviour; when production is fixed, flip the
     * assertion to 403.
     */
    @Test
    void pendingSessionsCanReachLookupApi_currentlyAllowed() throws Exception {
        List<Route> lookup = routes().stream().filter(r -> r.pattern().startsWith("/api/lookup/")).toList();
        assertThat(lookup).isNotEmpty();
        for (Route r : lookup) {
            for (String role : PENDING_ROLES) {
                int status = statusOf(httpMethod(r), RouteSweep.fill(r.pattern()), as(role));
                assertThat(blocked(status)).as("FINDING: " + role + " " + where(r) + " -> " + status).isFalse();
            }
            assertThat(statusOf(httpMethod(r), RouteSweep.fill(r.pattern()), null)).isEqualTo(401);
        }
    }

    @Test
    void securityQuestionSetupPathsAcceptPendingSetupAndRealRoles() throws Exception {
        record Call(HttpMethod method, String path) {}
        List<Call> calls = List.of(
                new Call(HttpMethod.POST, "/api/account/security-questions/setup"),
                new Call(HttpMethod.GET, "/api/account/security-questions/default-questions"));
        for (Call c : calls) {
            for (String role : List.of("PENDING_SETUP", "ADMIN", "REGISTRAR", "TRAINER")) {
                assertThat(blocked(statusOf(c.method(), c.path(), as(role)))).as(role + " " + c.path()).isFalse();
            }
            for (String role : List.of("PENDING_VERIFICATION", "PENDING_RESET")) {
                assertThat(statusOf(c.method(), c.path(), as(role))).as(role + " " + c.path()).isEqualTo(403);
            }
            assertThat(statusOf(c.method(), c.path(), null)).as("anonymous " + c.path()).isEqualTo(401);
        }
    }

    @Test
    void recoveryStepsRequireTheirOwnPendingRole() throws Exception {
        record Step(String path, String role) {}
        for (Step s : List.of(new Step("/api/password-recovery/verify", "PENDING_VERIFICATION"),
                new Step("/api/password-recovery/reset", "PENDING_RESET"))) {
            assertThat(blocked(statusOf(HttpMethod.POST, s.path(), as(s.role())))).as(s.path()).isFalse();
            for (String role : ALL_ROLES) {
                if (!role.equals(s.role())) {
                    assertThat(statusOf(HttpMethod.POST, s.path(), as(role))).as(role + " " + s.path()).isEqualTo(403);
                }
            }
            assertThat(statusOf(HttpMethod.POST, s.path(), null)).as("anonymous " + s.path()).isEqualTo(401);
        }
    }

    @Test
    void unmappedApiPathRequiresLogin() throws Exception {
        assertThat(statusOf(HttpMethod.GET, "/api/does-not-exist", null)).isEqualTo(401);
    }

    @Test
    void everyRegisteredApiRouteHasAnExpectedAccessRule() {
        List<Route> all = routes();
        assertThat(all).isNotEmpty();
        List<String> unclassified = all.stream()
                .filter(r -> expectedAccess(r.pattern()) == null)
                .map(SecurityMatrixWebMvcTest::where)
                .toList();
        assertThat(unclassified)
                .as("add these routes to expectedAccess so the matrix covers them")
                .isEmpty();
    }
}
