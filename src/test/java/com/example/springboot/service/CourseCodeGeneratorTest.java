package com.example.springboot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;

import org.junit.jupiter.api.Test;

class CourseCodeGeneratorTest {

    @Test
    void usesInitialsOfSignificantWords() {
        assertThat(CourseCodeGenerator.baseCode("Culinary Arts and Restaurant Services")).isEqualTo("CARS");
    }

    @Test
    void punctuationSeparatesWords() {
        assertThat(CourseCodeGenerator.baseCode("Food & Beverage Services")).isEqualTo("FBS");
    }

    @Test
    void stopWordsAreSkippedCaseInsensitively() {
        assertThat(CourseCodeGenerator.baseCode("Bread AND Pastry Production")).isEqualTo("BPP");
    }

    @Test
    void digitsAreKept() {
        assertThat(CourseCodeGenerator.baseCode("Cookery NC 2")).isEqualTo("CN2");
    }

    @Test
    void singleWordFallsBackToTheWholeWordUppercased() {
        assertThat(CourseCodeGenerator.baseCode("  Cookery ")).isEqualTo("COOKERY");
    }

    @Test
    void longCodesAreTruncatedToLeaveRoomForASuffix() {
        String name = "Alpha Bravo Charlie Delta Echo Foxtrot Golf Hotel India Juliet "
                + "Kilo Lima Mike November Oscar Papa Quebec Romeo Sierra Tango";
        assertThat(CourseCodeGenerator.baseCode(name)).isEqualTo("ABCDEFGHIJKLMNOPQ");
    }

    @Test
    void blankNameIsRejected() {
        assertThatThrownBy(() -> CourseCodeGenerator.baseCode("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void symbolOnlyNameIsRejected() {
        assertThatThrownBy(() -> CourseCodeGenerator.baseCode("&&&"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void uniqueCodeReturnsTheBaseWhenItIsFree() {
        assertThat(CourseCodeGenerator.uniqueCode("Culinary Arts and Restaurant Services", code -> false))
                .isEqualTo("CARS");
    }

    @Test
    void uniqueCodeAppendsTheFirstFreeNumericSuffix() {
        Set<String> taken = Set.of("CARS", "CARS2");
        assertThat(CourseCodeGenerator.uniqueCode("Culinary Arts and Restaurant Services", taken::contains))
                .isEqualTo("CARS3");
    }

    @Test
    void courseCodeCollisionAddsNumericSuffix() {
        Set<String> taken = new java.util.HashSet<>();
        String first = CourseCodeGenerator.uniqueCode("Culinary Arts", taken::contains);
        taken.add(first);
        String second = CourseCodeGenerator.uniqueCode("Culinary Arts", taken::contains);
        taken.add(second);
        String third = CourseCodeGenerator.uniqueCode("Culinary Arts", taken::contains);

        assertThat(first).isEqualTo("CA");
        assertThat(second).isEqualTo("CA2");
        assertThat(third).isEqualTo("CA3");
    }
}
