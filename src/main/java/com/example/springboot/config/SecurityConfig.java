package com.example.springboot.config;

import java.io.IOException;
import java.util.Collection;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.example.springboot.service.CustomUserDetailsService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        "/",
                        "/index.html",
                        "/css/**",
                        "/js/**",
                        "/images/**"
                ).permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/student-portal.html", "/student-details.html").permitAll()
                .requestMatchers("/api/student-portal/**").permitAll()
                .requestMatchers("/api/student/**").permitAll()
                // Forgot-password flow — publicly reachable start (the email lookup
                // page itself, and the lookup call that issues a PENDING_VERIFICATION
                // session). The 3 steps after that are each gated to their own
                // short-lived, single-purpose synthetic role below, not permitAll —
                // see SecurityQuestionService / PasswordRecoveryController.
                .requestMatchers("/forgot-password.html", "/api/password-recovery/lookup").permitAll()
                // Mandatory security-question setup — a user who just logged in but
                // hasn't set up their 2 questions yet gets ONLY this synthetic role
                // (not their real ROLE_*), so they cannot reach any dashboard or API
                // until setup is complete.
                .requestMatchers("/security-question-setup.html",
                        "/api/account/security-questions/setup",
                        "/api/account/security-questions/default-questions").hasAnyRole(
                                "PENDING_SETUP", "ADMIN", "REGISTRAR", "TRAINER")
                .requestMatchers("/forgot-password-questions.html", "/api/password-recovery/verify")
                        .hasRole("PENDING_VERIFICATION")
                .requestMatchers("/reset-password.html", "/api/password-recovery/reset")
                        .hasRole("PENDING_RESET")
                // Role-based access — HTML pages
                .requestMatchers("/admin.html", "/logs.html", "/edit-user.html", "/add-user.html").hasRole("ADMIN")
                .requestMatchers("/registrar.html", "/subjects.html", "/student-records.html", "/classes.html", "/sections.html", "/documents.html", "/generate-document.html", "/student-numbers.html").hasRole("REGISTRAR")
                .requestMatchers("/trainer.html", "/trainer-subjects.html", "/trainer-classes.html").hasRole("TRAINER")
                // Role-based access — API endpoints
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/logs/**").hasRole("ADMIN")
                .requestMatchers("/api/registrar/**").hasRole("REGISTRAR")
                .requestMatchers("/api/trainer/**").hasRole("TRAINER")
                // Account endpoints require one of the real roles — deliberately NOT
                // just authenticated(), so a PENDING_SETUP/VERIFICATION/RESET session
                // (which IS "authenticated" as far as Spring Security is concerned)
                // cannot reach account settings, only the specific setup/verify/reset
                // paths carved out above.
                .requestMatchers("/api/account/**").hasAnyRole("ADMIN", "REGISTRAR", "TRAINER")
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(this::handleUnauthorized)
                .accessDeniedHandler(this::handleAccessDenied)
            )
            .headers(headers -> headers
                .cacheControl(cache -> cache.disable())
                // SAMEORIGIN (not the default DENY) so the Documents page can preview
                // stored PDFs/HTML in its same-origin <iframe> view modal.
                .frameOptions(frame -> frame.sameOrigin())
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return new ProviderManager(provider);
    }

    private void handleUnauthorized(HttpServletRequest request,
                                    HttpServletResponse response,
                                    org.springframework.security.core.AuthenticationException authException)
            throws IOException {
        if (isApiRequest(request)) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"message\":\"Unauthorized. Please log in.\"}");
        } else {
            response.sendRedirect("/index.html");
        }
    }

    private void handleAccessDenied(HttpServletRequest request,
                                    HttpServletResponse response,
                                    org.springframework.security.access.AccessDeniedException accessDeniedException)
            throws IOException {
        if (isApiRequest(request)) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"message\":\"Access denied. You do not have permission.\"}");
        } else {
            response.sendRedirect(getDashboardForCurrentUser());
        }
    }

    private boolean isApiRequest(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api/");
    }

    private String getDashboardForCurrentUser() {
        var authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
            for (GrantedAuthority authority : authorities) {
                return switch (authority.getAuthority()) {
                    case "ROLE_ADMIN" -> "/admin.html";
                    case "ROLE_REGISTRAR" -> "/registrar.html";
                    case "ROLE_TRAINER" -> "/trainer.html";
                    default -> "/index.html";
                };
            }
        }

        return "/index.html";
    }
}
