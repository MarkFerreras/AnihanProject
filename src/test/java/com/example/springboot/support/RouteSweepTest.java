package com.example.springboot.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class RouteSweepTest {

    @Test
    void fillReplacesEveryPlaceholder() {
        assertEquals("/a/1/b/1", RouteSweep.fill("/a/{id}/b/{x}"));
        assertEquals("/api/x", RouteSweep.fill("/api/x"));
    }
}
