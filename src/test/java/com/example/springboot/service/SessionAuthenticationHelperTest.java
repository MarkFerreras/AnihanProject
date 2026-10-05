package com.example.springboot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.springboot.model.User;

/**
 * Unit tests for {@link SessionAuthenticationHelper}.
 * ISO 25010 characteristic: Security (authenticity, access control).
 */
class SessionAuthenticationHelperTest {

    private final SessionAuthenticationHelper helper = new SessionAuthenticationHelper();

    @BeforeEach
    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    private List<String> authorities() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).toList();
    }

    private User trainer() {
        User u = new User();
        u.setUsername("trainer");
        u.setRole("ROLE_TRAINER");
        return u;
    }

    @Test
    void restrictedSessionGrantsOnlySyntheticRole() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        helper.establishRestrictedSession(req, "trainer", "PENDING_RESET");
        assertEquals(List.of("ROLE_PENDING_RESET"), authorities());
        assertEquals("trainer", SecurityContextHolder.getContext().getAuthentication().getName());
        assertNotNull(req.getSession(false).getAttribute("SPRING_SECURITY_CONTEXT"));
    }

    @Test
    void fullSessionGrantsRealRoleAndReplacesRestricted() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        helper.establishRestrictedSession(req, "trainer", "PENDING_RESET");
        helper.establishFullSession(req, trainer());
        assertEquals(List.of("ROLE_TRAINER"), authorities());
    }

    @Test
    void sessionIdChangesOnPrivilegeChange_currentlyUnchanged() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        helper.establishRestrictedSession(req, "trainer", "PENDING_RESET");
        String before = req.getSession(false).getId();
        helper.establishFullSession(req, trainer());
        String after = req.getSession(false).getId();
        // FINDING: SessionAuthenticationHelper never calls changeSessionId()/invalidate(), so the
        // session id is reused when privileges are elevated (session fixation exposure). Expected: differs.
        assertEquals(before, after);
    }
}
