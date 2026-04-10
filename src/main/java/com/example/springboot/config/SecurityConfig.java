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
                // Static resources and login page — always accessible
                .requestMatchers(
                    "/", "/index.html",
                    "/css/**", "/js/**", "/images/**"
                ).permitAll()
                // Auth endpoints — always accessible
                .requestMatchers("/api/auth/**").permitAll()
                // Role-based access for dashboard HTML pages
                .requestMatchers("/admin.html").hasRole("ADMIN")
                .requestMatchers("/registrar.html").hasRole("REGISTRAR")
                .requestMatchers("/trainer.html").hasRole("TRAINER")
                // Role-based access for API endpoints
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/registrar/**").hasRole("REGISTRAR")
                .requestMatchers("/api/trainer/**").hasRole("TRAINER")
                // Account endpoints require authentication (any role)
                .requestMatchers("/api/account/**").authenticated()
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(this::handleUnauthorized)
                .accessDeniedHandler(this::handleAccessDenied)
            )
            // Prevent caching of authenticated pages so back-button doesn't work after logout
            .headers(headers -> headers
                .cacheControl(cache -> cache.disable())
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

    /**
     * Handles unauthenticated access (401).
     * - API requests (/api/**) get a JSON 401 response.
     * - Browser page requests get redirected to /index.html.
     */
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

    /**
     * Handles access denied (403) for authenticated users accessing wrong-role resources.
     * - API requests get a JSON 403 response.
     * - Browser page requests get redirected to the user's own dashboard.
     */
    private void handleAccessDenied(HttpServletRequest request,
                                    HttpServletResponse response,
                                    org.springframework.security.access.AccessDeniedException accessDeniedException)
            throws IOException {
        if (isApiRequest(request)) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"message\":\"Access denied. You do not have permission.\"}");
        } else {
            // Redirect to the user's own dashboard based on their role
            String dashboard = getDashboardForCurrentUser(request);
            response.sendRedirect(dashboard);
        }
    }

    /**
     * Determines if the request is an API call (expects JSON) vs a browser page request.
     */
    private boolean isApiRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/api/");
    }

    /**
     * Returns the correct dashboard URL based on the authenticated user's role.
     */
    private String getDashboardForCurrentUser(HttpServletRequest request) {
        var authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();

        if (authentication != null && authentication.isAuthenticated()) {
            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
            for (GrantedAuthority authority : authorities) {
                String role = authority.getAuthority();
                return switch (role) {
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
