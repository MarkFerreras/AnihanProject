package com.example.springboot.service;

import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.example.springboot.model.User;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Issues the session for a user, in one of two shapes:
 *
 * <p><b>Full session</b> — the user's real role(s), used after a normal
 * login (once security-question setup is complete) and again at the end of
 * the forgot-password flow once a new password has been saved, so the user
 * lands directly on their dashboard without a second login.
 *
 * <p><b>Restricted, single-purpose session</b> — a synthetic role
 * (e.g. {@code ROLE_PENDING_SETUP}) that grants access to exactly one
 * follow-up step and nothing else. Used in three places, all governed by
 * matchers in {@code SecurityConfig}: after login when security questions
 * haven't been set up yet ({@code PENDING_SETUP}), after a successful
 * forgot-password email lookup ({@code PENDING_VERIFICATION}), and after
 * successfully answering both security questions ({@code PENDING_RESET}).
 * Each is scoped to one username via the authentication's principal name —
 * the follow-up endpoint reads {@code Authentication.getName()} rather than
 * trusting anything the client sends, so a request can't be replayed
 * against a different account.
 */
@Component
public class SessionAuthenticationHelper {

    public void establishFullSession(HttpServletRequest request, User user) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.getUsername(),
                null,
                List.of(new SimpleGrantedAuthority(user.getRole()))
        );
        setAuthentication(request, authentication);
    }

    /**
     * @param syntheticRole role name WITHOUT the {@code ROLE_} prefix, e.g. {@code "PENDING_SETUP"}
     */
    public void establishRestrictedSession(HttpServletRequest request, String username, String syntheticRole) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                username,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + syntheticRole))
        );
        setAuthentication(request, authentication);
    }

    private void setAuthentication(HttpServletRequest request, Authentication authentication) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
        HttpSession session = request.getSession(true);
        session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
    }
}
