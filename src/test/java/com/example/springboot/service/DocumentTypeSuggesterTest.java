package com.example.springboot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

/** Pins the alias list and the whole-word rule (spec 2026-10-01 SO checklist §9). */
class DocumentTypeSuggesterTest {

    private static void assertSuggests(String expectedType, List<String> labels) {
        for (String label : labels) {
            assertEquals(Optional.of(expectedType), DocumentTypeSuggester.suggest(label), label);
        }
    }

    @Test
    void torAliasesSuggestTheTor() {
        assertSuggests("Transcript of Records (TOR)",
                List.of("TOR", "tor scan", "Transcript", "transcript of records (copy)"));
    }

    @Test
    void psaAliasesSuggestThePsaBirthCertificate() {
        assertSuggests("PSA Birth Certificate", List.of("PSA", "Birth Cert", "NSO birth certificate"));
    }

    @Test
    void formIxAliasesAskForAFormIxType() {
        assertSuggests(DocumentTypeSuggester.FORM_IX_GENERIC, List.of("Form IX", "form 9", "Permanent Record"));
    }

    @Test
    void ojtAliasesSuggestTheOjtReport() {
        assertSuggests("OJT Report", List.of("OJT", "ojt report", "OJT Report - Jollibee"));
    }

    @Test
    void tvetAliasesSuggestTheTvetCertificate() {
        assertSuggests("Certificate of TVET Program", List.of("TVET", "tvet certificate"));
    }

    @Test
    void form137AliasesSuggestForm137() {
        assertSuggests("Form 137", List.of("Form 137", "F137"));
    }

    @Test
    void labelsThatMerelyContainAnAliasSuggestNothing() {
        for (String label : List.of("Director's letter", "Monitoring report", "History",
                "Torres family letter", "Medical Certificate", "Form 1370")) {
            assertTrue(DocumentTypeSuggester.suggest(label).isEmpty(), label);
        }
    }

    @Test
    void aTorRequestLetterIsStillSuggestedAsTorBecauseItIsOnlyAHint() {
        assertSuggests("Transcript of Records (TOR)", List.of("TOR request letter"));
    }

    @Test
    void blankOrNullLabelsSuggestNothing() {
        assertTrue(DocumentTypeSuggester.suggest(null).isEmpty());
        assertTrue(DocumentTypeSuggester.suggest("   ").isEmpty());
    }
}
