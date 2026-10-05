package com.example.springboot.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.LockedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.example.springboot.exception.EmptyDocumentExportException;
import com.example.springboot.exception.GlobalExceptionHandler;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * ISO 25010: Reliability (fault tolerance) and Security (no information leakage in error bodies).
 *
 * Standalone MockMvc: a stub controller throws the exception named in the path and the real
 * {@link GlobalExceptionHandler} maps it. No Spring context is started.
 */
class GlobalExceptionHandlerWebMvcTest {

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(new ThrowingStubController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @RestController
    static class ThrowingStubController {

        record Payload(@NotBlank(message = "Name is required") String name,
                       @Size(min = 3, message = "Code must be at least 3 characters") String code) { }

        @PostMapping("/stub/validate")
        String validate(@Valid @RequestBody Payload body) {
            return "ok";
        }

        @GetMapping("/stub/number/{id}")
        String number(@PathVariable Integer id) {
            return "ok";
        }

        @GetMapping("/stub/throw/{kind}")
        String throwing(@PathVariable String kind) throws Exception {
            switch (kind) {
                case "bad-credentials" -> throw new BadCredentialsException("user 'ghost' not found");
                case "locked" -> throw new LockedException("locked");
                case "disabled" -> throw new DisabledException("disabled");
                case "access-denied" -> throw new AccessDeniedException("nope");
                case "authentication" -> throw new InsufficientAuthenticationException("auth");
                case "illegal-argument" -> throw new IllegalArgumentException("Section name is required");
                case "no-such-element" -> throw new NoSuchElementException("Student not found");
                case "empty-export" -> throw new EmptyDocumentExportException("No documents to export");
                case "no-resource" -> throw new NoResourceFoundException(HttpMethod.GET, "/missing.js", "missing.js");
                case "integrity" -> throw new DataIntegrityViolationException(
                        "could not execute statement; SQL [insert into users]; constraint [uk_users_username]");
                case "too-large" -> throw new MaxUploadSizeExceededException(1L);
                case "missing-param" -> throw new MissingServletRequestParameterException("batchId", "Integer");
                case "missing-part" -> throw new MissingServletRequestPartException("file");
                case "boom" -> throw new RuntimeException("boom");
                default -> { return "ok"; }
            }
        }
    }

    // T3-01
    @Test
    void validationErrorReturns400WithFieldErrors() throws Exception {
        mvc.perform(post("/stub/validate").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"code\":\"ab\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.name").value("Name is required"))
                .andExpect(jsonPath("$.errors.code").value("Code must be at least 3 characters"));
    }

    // T3-02
    @Test
    void badCredentialsReturns401() throws Exception {
        mvc.perform(get("/stub/throw/bad-credentials"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"))
                .andExpect(content().string(not(containsString("ghost"))));
    }

    // T3-03
    @Test
    void lockedAccountReturns401WithLockedMessage() throws Exception {
        mvc.perform(get("/stub/throw/locked"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(containsString("locked")));
    }

    // T3-04
    @Test
    void disabledAccountReturns401WithDisabledMessage() throws Exception {
        mvc.perform(get("/stub/throw/disabled"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(containsString("deactivated")));
    }

    // T3-05
    @Test
    void accessDeniedReturns403() throws Exception {
        mvc.perform(get("/stub/throw/access-denied"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(containsString("Access denied")));
    }

    // T3-06
    @Test
    void authenticationExceptionReturns401() throws Exception {
        mvc.perform(get("/stub/throw/authentication"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Unauthorized. Please log in."));
    }

    // T3-07
    @Test
    void illegalArgumentReturns400WithMessage() throws Exception {
        mvc.perform(get("/stub/throw/illegal-argument"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Section name is required"));
    }

    // T3-08
    @Test
    void noSuchElementReturns404() throws Exception {
        mvc.perform(get("/stub/throw/no-such-element"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Student not found"));
    }

    // T3-09
    @Test
    void emptyDocumentExportReturns409() throws Exception {
        mvc.perform(get("/stub/throw/empty-export"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("No documents to export"));
    }

    // T3-10
    @Test
    void missingStaticResourceReturns404() throws Exception {
        mvc.perform(get("/stub/throw/no-resource"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("Resource not found")));
    }

    // T3-11
    @Test
    void dataIntegrityViolationReturns409WithoutSqlDetails() throws Exception {
        mvc.perform(get("/stub/throw/integrity"))
                .andExpect(status().isConflict())
                .andExpect(content().string(not(containsString("SQL"))))
                .andExpect(content().string(not(containsString("uk_users_username"))))
                .andExpect(content().string(not(containsString("insert into"))));
    }

    // T3-12
    // FINDING: HttpMessageNotReadableException is not handled specifically, so it falls into the
    // catch-all Exception handler. Expected 400, observed 500.
    @Test
    void malformedJsonBodyReturnsClientError_currentlyServerError() throws Exception {
        mvc.perform(post("/stub/validate").contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isInternalServerError());
    }

    // T3-13
    // FINDING: MethodArgumentTypeMismatchException is not handled specifically, so it falls into
    // the catch-all Exception handler. Expected 400, observed 500.
    @Test
    void nonNumericPathOrQueryValueReturnsClientError_currentlyServerError() throws Exception {
        mvc.perform(get("/stub/number/abc"))
                .andExpect(status().isInternalServerError());
    }

    // T3-14
    @Test
    void oversizedUploadReturns400() throws Exception {
        mvc.perform(get("/stub/throw/too-large"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("maximum allowed size")));
    }

    // T3-15
    @Test
    void missingRequestParamReturns400() throws Exception {
        mvc.perform(get("/stub/throw/missing-param"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Missing required parameter: batchId"));
    }

    // T3-16
    @Test
    void missingMultipartPartReturns400() throws Exception {
        mvc.perform(get("/stub/throw/missing-part"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Missing required part: file"));
    }

    // T3-17
    @Test
    void unexpectedExceptionReturns500WithoutStackTrace() throws Exception {
        mvc.perform(get("/stub/throw/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value(containsString("unexpected error")))
                .andExpect(content().string(not(containsString("boom"))))
                .andExpect(content().string(not(containsString("RuntimeException"))))
                .andExpect(content().string(not(containsString("at com.example"))));
    }
}
