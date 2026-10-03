package com.example.springboot.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Suggests a real document type for an "Others" label that looks like one (spec 2026-10-01
 * SO checklist §9), e.g. "TOR copy" → Transcript of Records, so it can be refiled before it
 * silently fails to count on the SO checklist. A hint only: the upload page never
 * auto-switches and never blocks. An alias matches only as a whole word, case-insensitive
 * ("Director's letter" does not match "tor"). All alias knowledge lives here, the same idea
 * as StudentNumberImportMapping: to catch a new spelling, add it below and re-run
 * DocumentTypeSuggesterTest.
 */
public final class DocumentTypeSuggester {

    /** Returned for Form IX aliases: there are three Form IX types, so the Registrar picks one. */
    public static final String FORM_IX_GENERIC = "Form IX";

    // Checked in this order; the first alias found wins.
    private static final List<Map.Entry<String, List<String>>> ALIASES = List.of(
            Map.entry(DocumentService.TOR_TYPE, List.of("transcript of records", "transcript", "tor")),
            Map.entry(DocumentService.PSA_BIRTH_CERTIFICATE_TYPE, List.of("birth certificate", "birth cert", "psa")),
            Map.entry(FORM_IX_GENERIC, List.of("permanent record", "form ix", "form 9")),
            Map.entry(DocumentService.OJT_REPORT_TYPE, List.of("ojt report", "ojt")),
            Map.entry(DocumentService.TVET_CERTIFICATE_TYPE, List.of("tvet certificate", "tvet")),
            Map.entry(DocumentService.FORM_137_TYPE, List.of("form 137", "f137")));

    private static final List<Map.Entry<String, Pattern>> PATTERNS = compile();

    private DocumentTypeSuggester() {
    }

    public static Optional<String> suggest(String label) {
        if (label == null || label.isBlank()) {
            return Optional.empty();
        }
        for (Map.Entry<String, Pattern> entry : PATTERNS) {
            if (entry.getValue().matcher(label).find()) {
                return Optional.of(entry.getKey());
            }
        }
        return Optional.empty();
    }

    /** "birth cert" → (?<![\p{L}\p{N}])\Qbirth\E\s+\Qcert\E(?![\p{L}\p{N}]) — whole words, any spacing. */
    private static List<Map.Entry<String, Pattern>> compile() {
        List<Map.Entry<String, Pattern>> patterns = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : ALIASES) {
            for (String alias : entry.getValue()) {
                String words = Arrays.stream(alias.split(" "))
                        .map(Pattern::quote)
                        .collect(Collectors.joining("\\s+"));
                patterns.add(Map.entry(entry.getKey(), Pattern.compile(
                        "(?<![\\p{L}\\p{N}])" + words + "(?![\\p{L}\\p{N}])",
                        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)));
            }
        }
        return List.copyOf(patterns);
    }
}
