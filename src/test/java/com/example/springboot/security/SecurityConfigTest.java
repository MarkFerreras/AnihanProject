package com.example.springboot.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.example.springboot.config.SecurityConfig;
import com.example.springboot.controller.AdminController;
import com.example.springboot.exception.GlobalExceptionHandler;
import com.example.springboot.repository.UserRepository;
import com.example.springboot.service.AdminService;
import com.example.springboot.service.CustomUserDetailsService;
import com.example.springboot.service.SystemLogService;

/**
 * Security configuration: error handlers, CSRF stance, password encoder, headers, and the
 * session/JPA/multipart properties that protect production data.
 * <p>
 * ISO 25010: Security
 */
@WebMvcTest(AdminController.class)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
class SecurityConfigTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private PasswordEncoder passwordEncoder;

    @MockitoBean private CustomUserDetailsService customUserDetailsService;
    @MockitoBean private AdminService adminService;
    @MockitoBean private SystemLogService systemLogService;
    @MockitoBean private UserRepository userRepository;

    private static Properties appProperties() throws IOException {
        Properties p = new Properties();
        try (InputStream in = new ClassPathResource("application.properties").getInputStream()) {
            p.load(in);
        }
        return p;
    }

    @Test
    void apiUnauthorizedReturnsJson401() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("{\"message\":\"Unauthorized. Please log in.\"}", true));
    }

    @Test
    void pageUnauthorizedRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin.html"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/index.html"));
    }

    @Test
    void apiForbiddenReturnsJson403() throws Exception {
        MvcResult res = mockMvc.perform(get("/api/admin/users").with(user("t").roles("TRAINER")))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();
        assertThat(res.getResponse().getContentAsString()).contains("Access denied");
    }

    @Test
    void pageForbiddenRedirectsToOwnDashboard() throws Exception {
        mockMvc.perform(get("/admin.html").with(user("t").roles("TRAINER")))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/trainer.html"));
        mockMvc.perform(get("/admin.html").with(user("r").roles("REGISTRAR")))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/registrar.html"));
    }

    @Test
    void csrfIsDisabledByDesign() throws Exception {
        // No CSRF token supplied: the filter must not reject with 403 (project is AJAX/JSON only).
        int status = mockMvc.perform(post("/api/admin/users")
                        .with(user("a").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andReturn().getResponse().getStatus();
        assertThat(status).isNotEqualTo(403).isNotEqualTo(401);
    }

    @Test
    void passwordEncoderIsBcryptAndNotReversible() {
        String encoded = passwordEncoder.encode("password123");
        assertThat(encoded).isNotEqualTo("password123").startsWith("$2");
        assertThat(passwordEncoder.matches("password123", encoded)).isTrue();
        assertThat(passwordEncoder.matches("wrong", encoded)).isFalse();
        assertThat(passwordEncoder.encode("password123")).isNotEqualTo(encoded);
    }

    @Test
    void frameOptionsAreSameOrigin() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(header().string("X-Frame-Options", "SAMEORIGIN"));
    }

    @Test
    void cacheControlHeadersDisabledAsConfigured() throws Exception {
        // Pins current config (cacheControl disabled): Spring Security adds no Cache-Control/Pragma/Expires.
        MvcResult res = mockMvc.perform(get("/api/admin/users")).andReturn();
        assertThat(res.getResponse().getHeader("Cache-Control")).isNull();
        assertThat(res.getResponse().getHeader("Pragma")).isNull();
        assertThat(res.getResponse().getHeader("Expires")).isNull();
    }

    @Test
    void sessionCookieIsHttpOnlyLaxAnd30Minutes() throws Exception {
        Properties p = appProperties();
        assertThat(p.getProperty("server.servlet.session.cookie.http-only")).isEqualTo("true");
        assertThat(p.getProperty("server.servlet.session.cookie.same-site")).isEqualToIgnoringCase("lax");
        assertThat(p.getProperty("server.servlet.session.timeout")).isEqualTo("30m");
    }

    @Test
    void jpaDdlIsNone() throws Exception {
        assertThat(appProperties().getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("none");
    }

    @Test
    void multipartLimitsMatchDocumentedValues() throws Exception {
        Properties p = appProperties();
        assertThat(p.getProperty("spring.servlet.multipart.max-file-size")).isEqualTo("10MB");
        assertThat(p.getProperty("spring.servlet.multipart.max-request-size")).isEqualTo("52MB");
    }
}
