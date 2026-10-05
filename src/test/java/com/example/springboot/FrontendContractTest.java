package com.example.springboot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.example.springboot.dto.registrar.StudentRecordUpdateRequest;
import com.example.springboot.dto.registrar.UpdateStudentStatusRequest;
import com.example.springboot.service.StudentStatusTransitions;
import com.example.springboot.support.RouteSweep;

/**
 * Pins static HTML choices that mirror server-side constants, so the two cannot drift
 * (spec 2026-10-01 SO checklist §4.4 and §5).
 * <p>
 * ISO 25010: Maintainability, Compatibility (air-gapped, no CDN) and Functional suitability.
 */
class FrontendContractTest {

    private static final String STATIC = "src/main/resources/static/";

    /** The option values of the select with this id, in page order. */
    static List<String> optionValues(String page, String selectId) throws Exception {
        String html = Files.readString(Path.of(STATIC + page));
        Matcher select = Pattern.compile("(?s)<select[^>]*id=\"" + selectId + "\"[^>]*>(.*?)</select>").matcher(html);
        assertTrue(select.find(), page + " has no #" + selectId);
        Matcher option = Pattern.compile("<option value=\"([^\"]*)\"").matcher(select.group(1));
        List<String> values = new ArrayList<>();
        while (option.find()) {
            values.add(option.group(1));
        }
        return values;
    }

    @Test
    void statusFiltersOfferCompletedBetweenActiveAndGraduated() throws Exception {
        for (String page : List.of("registrar.html", "student-numbers.html")) {
            assertEquals(List.of("", "Enrolling", "Submitted", "Active", "Completed", "Graduated"),
                    optionValues(page, "studentStatusFilter"), page);
        }
    }

    @Test
    void editStatusOptionsMatchTheServerAllowedValues() throws Exception {
        assertEquals(List.of(UpdateStudentStatusRequest.ALLOWED_VALUES_DISPLAY.split(", ")),
                optionValues("registrar.html", "editStatusSelect"));
    }

    @Test
    void employmentStatusOptionsMatchTheServerPattern() throws Exception {
        List<String> expected = new ArrayList<>();
        expected.add(""); // "Not set"
        expected.addAll(List.of(StudentRecordUpdateRequest.EMPLOYMENT_STATUS_DISPLAY.split(", ")));
        assertEquals(expected, optionValues("student-records.html", "editEmploymentStatus"));
    }

    /**
     * RegistrarService.applyDates rejects a changed completion date for a non-Completed student and
     * clears a null enrollment date, so the edit form must always send all three loaded values back
     * (spec 2026-10-01 SO checklist §4.3).
     */
    @Test
    void editFormPayloadAlwaysSendsTheDatesAndEmploymentStatus() throws Exception {
        String js = Files.readString(Path.of(STATIC + "js/registrar-student-records-edit.js"));
        Matcher fn = Pattern.compile("(?s)function buildPayload\\(\\)\\s*\\{(.*?)\\n    \\}").matcher(js);
        assertTrue(fn.find(), "registrar-student-records-edit.js has no buildPayload()");
        String body = fn.group(1);

        String html = Files.readString(Path.of(STATIC + "student-records.html"));
        for (String[] field : new String[][] {
                { "enrollmentDate", "editEnrollmentDate" },
                { "completionDate", "editCompletionDate" },
                { "employmentStatus", "editEmploymentStatus" } }) {
            assertTrue(Pattern.compile("(?m)^\\s*" + field[0] + ":\\s*.*'" + field[1] + "'").matcher(body).find(),
                    "buildPayload() must send " + field[0] + " from #" + field[1]);
            assertTrue(html.contains("id=\"" + field[1] + "\""), "student-records.html has no #" + field[1]);
        }
    }

    // ---- Task 10 additions (ISO 25010: Maintainability, Compatibility, Security) ----

    private static final List<String> STATUSES = List.of("Enrolling", "Submitted", "Active", "Completed", "Graduated");

    private static List<String> pages() throws Exception {
        try (Stream<Path> files = Files.list(Path.of(STATIC))) {
            return files.map(f -> f.getFileName().toString()).filter(n -> n.endsWith(".html")).sorted().toList();
        }
    }

    /** Pages behind login: the ones that declare a required role. */
    private static List<String> protectedPages() throws Exception {
        List<String> result = new ArrayList<>();
        for (String page : pages()) {
            if (Files.readString(Path.of(STATIC + page)).contains("data-required-role=")) {
                result.add(page);
            }
        }
        return result;
    }

    /**
     * T10-08. FINDING: edit-user.html loads auth-guard.js but no jQuery, against the CLAUDE.md page
     * checklist ("auth-guard.js + jQuery imports"). auth-guard.js itself does not use jQuery today,
     * so nothing is broken at runtime; the omission is pinned in an allow-list.
     */
    @Test
    void everyAuthenticatedPageLoadsAuthGuardAndJquery() throws Exception {
        List<String> pages = protectedPages();
        assertTrue(pages.size() >= 15, "expected the protected pages, found " + pages);
        List<String> noGuard = new ArrayList<>();
        List<String> noJquery = new ArrayList<>();
        for (String page : pages) {
            String html = Files.readString(Path.of(STATIC + page));
            if (!Pattern.compile("<script[^>]+src=\"js/auth-guard\\.js").matcher(html).find()) {
                noGuard.add(page);
            }
            if (!Pattern.compile("<script[^>]+src=\"js/jquery[^\"]*\\.js").matcher(html).find()) {
                noJquery.add(page);
            }
        }
        assertEquals(List.of(), noGuard);
        assertEquals(List.of("edit-user.html"), noJquery);
    }

    /** T10-09 */
    @Test
    void everyDashboardPageHasNavbarAccountDropdownAndEditModal() throws Exception {
        for (String page : protectedPages()) {
            String html = Files.readString(Path.of(STATIC + page));
            assertTrue(html.contains("<nav"), page + " has no navbar");
            assertTrue(html.contains("account-icon-btn"), page + " has no account dropdown");
            assertTrue(html.contains("id=\"editAccountModal\""), page + " has no editAccountModal");
        }
    }

    /** T10-10 */
    @Test
    void studentPortalHasNoFileInput() throws Exception {
        for (String page : List.of("student-portal.html", "student-details.html")) {
            String html = Files.readString(Path.of(STATIC + page));
            assertTrue(!Pattern.compile("(?i)type\\s*=\\s*[\"']file[\"']").matcher(html).find(), page);
        }
        for (String js : List.of("student-portal.js", "student-details.js")) {
            assertTrue(!Files.readString(Path.of(STATIC + "js/" + js)).contains("FormData"), js);
        }
    }

    /** T10-11 */
    @Test
    void noDatalistAnywhere() throws Exception {
        for (String page : pages()) {
            assertTrue(!Files.readString(Path.of(STATIC + page)).toLowerCase().contains("<datalist"), page);
        }
    }

    /** T10-12 */
    @Test
    void noCdnReferences() throws Exception {
        Pattern external = Pattern.compile(
                "(?is)<(?:script|link)\\b[^>]*\\b(?:src|href)\\s*=\\s*[\"'](?:https?:)?//[^\"']*[\"']");
        for (String page : pages()) {
            assertTrue(!external.matcher(Files.readString(Path.of(STATIC + page))).find(), page);
        }
        Pattern cssExternal = Pattern.compile("(?i)(@import\\s+(?:url\\()?\\s*[\"']?|url\\(\\s*[\"']?)(?:https?:)?//");
        try (Stream<Path> css = Files.list(Path.of(STATIC + "css"))) {
            for (Path file : css.filter(f -> f.toString().endsWith(".css")).toList()) {
                assertTrue(!cssExternal.matcher(Files.readString(file)).find(), file.toString());
            }
        }
    }

    /** Builds the real /api/** route table without starting the application context. */
    private static List<RouteSweep.Route> apiRoutes() throws Exception {
        StaticApplicationContext ctx = new StaticApplicationContext();
        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Controller.class));
        for (var def : scanner.findCandidateComponents("com.example.springboot.controller")) {
            RootBeanDefinition bean = new RootBeanDefinition(Class.forName(def.getBeanClassName()));
            bean.setLazyInit(true);
            ctx.registerBeanDefinition(def.getBeanClassName(), bean);
        }
        ctx.refresh();
        RequestMappingHandlerMapping mapping = new RequestMappingHandlerMapping();
        mapping.setApplicationContext(ctx);
        mapping.afterPropertiesSet();
        return RouteSweep.allApiRoutes(mapping);
    }

    /**
     * T10-13. Every "/api/..." string literal in static/js must resolve to a registered route
     * (query string dropped, {@code ${x}} treated as a path variable, a literal followed by "+" treated
     * as a prefix). Method is not compared. Assertion is against an empty allow-list.
     */
    @Test
    void everyFetchedApiUrlMatchesARealMapping() throws Exception {
        List<RouteSweep.Route> routes = apiRoutes();
        assertTrue(routes.size() > 80, "route table looks wrong: " + routes.size());
        List<Pattern> exact = new ArrayList<>();
        List<String> filled = new ArrayList<>();
        for (RouteSweep.Route r : routes) {
            exact.add(Pattern.compile(r.pattern().replaceAll("\\{[^}]*\\}", "[^/]+")));
            filled.add(RouteSweep.fill(r.pattern()));
        }
        Pattern literal = Pattern.compile("([\"'`])(/api/[^\"'`\\n]*)\\1(\\s*\\+)?");
        List<String> dead = new ArrayList<>();
        int seen = 0;
        try (Stream<Path> js = Files.list(Path.of(STATIC + "js"))) {
            for (Path file : js.filter(f -> f.toString().endsWith(".js") && !f.toString().contains(".min."))
                    .sorted().toList()) {
                Matcher m = literal.matcher(Files.readString(file));
                while (m.find()) {
                    seen++;
                    String url = m.group(2).replaceAll("\\$\\{[^}]*\\}", "1");
                    int q = url.indexOf('?');
                    if (q >= 0) {
                        url = url.substring(0, q);
                    }
                    boolean prefix = m.group(3) != null;
                    String candidate = url;
                    boolean ok = false;
                    for (int i = 0; i < exact.size() && !ok; i++) {
                        ok = exact.get(i).matcher(candidate).matches()
                                || ((prefix || candidate.endsWith("/")) && filled.get(i).startsWith(candidate));
                    }
                    if (!ok) {
                        dead.add(file.getFileName() + ": " + m.group(2));
                    }
                }
            }
        }
        assertTrue(seen > 30, "URL scan looks broken: " + seen);
        assertEquals(List.of(), dead);
    }

    /**
     * Tiny interpreter for the exact shape of {@code statusRule} in registrar-students.js: sequential
     * {@code if (from === 'X') return {...};}, {@code if (to === 'X') { ... }} blocks and a final
     * {@code return {...};}. Fails loudly if the function takes a shape it does not understand.
     */
    private static Map<String, Boolean> evalStatusRule(List<String> lines, String from, String to) {
        int depth = 0;
        int skipDepth = -1;
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty()) {
                continue;
            }
            boolean skipping = skipDepth >= 0;
            if (line.equals("}")) {
                depth--;
                if (skipping && depth == skipDepth) {
                    skipDepth = -1;
                }
                continue;
            }
            Matcher block = Pattern.compile("if \\(to === '(\\w+)'\\) \\{").matcher(line);
            if (block.matches()) {
                if (!skipping && !to.equals(block.group(1))) {
                    skipDepth = depth;
                }
                depth++;
                continue;
            }
            if (skipping) {
                continue;
            }
            Matcher cond = Pattern.compile("if \\((.+?)\\) return (\\{.*\\});").matcher(line);
            if (cond.matches()) {
                String c = cond.group(1);
                boolean hit;
                if (c.equals("from === to")) {
                    hit = from.equals(to);
                } else if (c.matches("from === '\\w+'")) {
                    hit = c.equals("from === '" + from + "'");
                } else {
                    throw new AssertionError("unsupported condition: " + line);
                }
                if (hit) {
                    return parseRule(cond.group(2), to);
                }
                continue;
            }
            Matcher ret = Pattern.compile("return (\\{.*\\});").matcher(line);
            if (ret.matches()) {
                return parseRule(ret.group(1), to);
            }
            throw new AssertionError("unsupported statement: " + line);
        }
        throw new AssertionError("statusRule fell through for " + from + " -> " + to);
    }

    private static Map<String, Boolean> parseRule(String object, String to) {
        Map<String, Boolean> rule = new HashMap<>();
        rule.put("allowed", false);
        rule.put("needsDate", false);
        rule.put("needsReason", false);
        Matcher prop = Pattern.compile("(\\w+):\\s*([^,}]+?)\\s*(?=,|\\})").matcher(object);
        while (prop.find()) {
            String key = prop.group(1);
            String value = prop.group(2);
            if (!rule.containsKey(key)) {
                continue; // archive etc. have no Java counterpart
            }
            if (value.equals("true") || value.equals("false")) {
                rule.put(key, Boolean.parseBoolean(value));
            } else if (value.matches("to === '\\w+'")) {
                rule.put(key, value.equals("to === '" + to + "'"));
            } else {
                throw new AssertionError("unsupported value: " + value);
            }
        }
        return rule;
    }

    /** T10-14 */
    @Test
    void statusRulesInJsMatchJavaMatrix() throws Exception {
        String js = Files.readString(Path.of(STATIC + "js/registrar-students.js"));
        Matcher fn = Pattern.compile("(?s)function statusRule\\(from, to\\) \\{(.*?)\\n    \\}").matcher(js);
        assertTrue(fn.find(), "registrar-students.js has no statusRule(from, to)");
        List<String> lines = List.of(fn.group(1).split("\\R"));

        for (String from : STATUSES) {
            for (String to : STATUSES) {
                var java = StudentStatusTransitions.check(from, to);
                Map<String, Boolean> rule = evalStatusRule(lines, from, to);
                String pair = from + " -> " + to;
                assertEquals(java.allowed(), rule.get("allowed"), pair + " allowed");
                if (java.allowed()) {
                    assertEquals(java.requiresCompletionDate(), rule.get("needsDate"), pair + " date");
                    assertEquals(java.requiresReason(), rule.get("needsReason"), pair + " reason");
                }
            }
        }
    }
}
