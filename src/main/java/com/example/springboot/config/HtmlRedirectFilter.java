package com.example.springboot.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

/**
 * Issues 301 redirects from legacy .html URLs to their clean equivalents.
 * Registered as a @Component so Spring Boot only applies it to DispatcherType.REQUEST
 * (real browser requests), never to internal RequestDispatcher forwards — avoiding
 * the redirect loop that occurs when addRedirectViewController intercepts forwards.
 */
@Component
public class HtmlRedirectFilter extends OncePerRequestFilter {

    private static final Map<String, String> HTML_TO_CLEAN = Map.of(
            "/index.html",          "/index",
            "/admin.html",          "/admin",
            "/registrar.html",      "/registrar",
            "/trainer.html",        "/trainer",
            "/logs.html",           "/logs",
            "/subjects.html",       "/subjects",
            "/student-records.html","/student-records",
            "/add-user.html",       "/add-user",
            "/edit-user.html",      "/edit-user"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String target = HTML_TO_CLEAN.get(request.getRequestURI());
        if (target != null) {
            String qs = request.getQueryString();
            response.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
            response.setHeader("Location", qs != null ? target + "?" + qs : target);
            return;
        }
        chain.doFilter(request, response);
    }
}
