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
                // Role-based access
                // Role-based access for dashboard HTML pages
                .requestMatchers("/admin.html").hasRole("ADMIN")
                .requestMatchers("/registrar.html").hasRole("REGISTRAR")
                .requestMatchers("/trainer.html").hasRole("TRAINER")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/logs/**").hasRole("ADMIN")
                .requestMatchers("/api/registrar/**").hasRole("REGISTRAR")
                .requestMatchers("/api/trainer/**").hasRole("TRAINER")
                // Role-based access for HTML pages
                .requestMatchers("/admin.html", "/student-records.html", "/subjects.html", "/logs.html", "/edit-user.html", "/add-user.html").hasRole("ADMIN")
                .requestMatchers("/registrar.html").hasRole("REGISTRAR")
                .requestMatchers("/trainer.html").hasRole("TRAINER")
                // Account endpoints require authentication (any role)
                .requestMatchers("/api/account/**").authenticated()
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(this::handleUnauthorized)
                .accessDeniedHandler(this::handleAccessDenied)
            )
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
