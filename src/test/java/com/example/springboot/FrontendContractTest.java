package com.example.springboot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import com.example.springboot.dto.registrar.StudentRecordUpdateRequest;
import com.example.springboot.dto.registrar.UpdateStudentStatusRequest;

/**
 * Pins static HTML choices that mirror server-side constants, so the two cannot drift
 * (spec 2026-10-01 SO checklist §4.4 and §5).
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
}
