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
}
