package com.example.springboot.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.example.springboot.model.User;
import com.example.springboot.repository.UserRepository;

/**
 * Unit tests for {@link CustomUserDetailsService}.
 * ISO 25010 characteristic: Security (authenticity, access control).
 */
@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    private User user(boolean enabled, boolean locked) {
        User u = new User();
        u.setUsername("registrar");
        u.setPassword("hash");
        u.setEmail("reg@anihan.test");
        u.setRole("ROLE_REGISTRAR");
        u.setEnabled(enabled);
        u.setSecurityLocked(locked);
        return u;
    }

    private UserDetails load(User u) {
        when(userRepository.findByUsernameOrEmail("registrar", "registrar")).thenReturn(Optional.of(u));
        return new CustomUserDetailsService(userRepository).loadUserByUsername("registrar");
    }

    @Test
    void unknownUsernameThrowsUsernameNotFound() {
        when(userRepository.findByUsernameOrEmail("ghost", "ghost")).thenReturn(Optional.empty());
        assertThrows(UsernameNotFoundException.class,
                () -> new CustomUserDetailsService(userRepository).loadUserByUsername("ghost"));
    }

    @Test
    void roleIsExposedAsGrantedAuthority() {
        UserDetails d = load(user(true, false));
        List<String> auths = d.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        // hasRole("REGISTRAR") requires the stored value to already carry the ROLE_ prefix
        assertEquals(List.of("ROLE_REGISTRAR"), auths);
    }

    @Test
    void disabledUserIsReportedDisabled() {
        UserDetails d = load(user(false, false));
        assertFalse(d.isEnabled());
    }

    @Test
    void lockedUserIsReportedLocked() {
        UserDetails d = load(user(true, true));
        assertFalse(d.isAccountNonLocked());
        assertTrue(d.isEnabled());
    }
}
