package com.example.springboot.controller;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.springboot.dto.trainer.GradeSummaryResponse;
import com.example.springboot.dto.trainer.SaveGradeRequest;
import com.example.springboot.model.User;
import com.example.springboot.repository.UserRepository;
import com.example.springboot.service.SystemLogService;
import com.example.springboot.service.TrainerGradeService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/trainer/classes/{classId}/grades")
@PreAuthorize("hasRole('TRAINER')")
public class TrainerGradeController {

    private static final Logger log = LoggerFactory.getLogger(TrainerGradeController.class);

    private final TrainerGradeService gradeService;
    private final SystemLogService systemLogService;
    private final UserRepository userRepository;

    public TrainerGradeController(TrainerGradeService gradeService,
                                  SystemLogService systemLogService,
                                  UserRepository userRepository) {
        this.gradeService = gradeService;
        this.systemLogService = systemLogService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<?> getGrades(@PathVariable Integer classId) {
        try {
            GradeSummaryResponse response = gradeService.getGradesForClass(classId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to get grades for class {}: {}", classId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping
    public ResponseEntity<?> saveGrades(
            @PathVariable Integer classId,
            @RequestBody List<SaveGradeRequest> gradeUpdates,
            HttpServletRequest httpRequest) {
        try {
            gradeService.saveGrades(classId, gradeUpdates);
            int count = gradeUpdates != null ? gradeUpdates.size() : 0;
            logGradeAction("Saved grades for class #" + classId + " (" + count + " student(s))", httpRequest);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.warn("Failed to save grades for class {}: {}", classId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/lock")
    public ResponseEntity<?> lockGrades(@PathVariable Integer classId, HttpServletRequest httpRequest) {
        try {
            gradeService.lockGrades(classId);
            logGradeAction("Locked grades for class #" + classId, httpRequest);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.warn("Failed to lock grades for class {}: {}", classId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/unlock")
    public ResponseEntity<?> unlockGrades(@PathVariable Integer classId, HttpServletRequest httpRequest) {
        try {
            gradeService.unlockGrades(classId);
            logGradeAction("Unlocked grades for class #" + classId, httpRequest);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.warn("Failed to unlock grades for class {}: {}", classId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * Writes an append-only audit row for a grade action. Grade input, locking,
     * and unlocking are all significant actions per the system-logs policy.
     */
    private void logGradeAction(String action, HttpServletRequest httpRequest) {
        LogContext ctx = getLogContext();
        systemLogService.logAction(ctx.userId(), ctx.username(), ctx.role(),
                action, httpRequest.getRemoteAddr());
    }

    private LogContext getLogContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        String role = auth.getAuthorities().stream()
                .findFirst()
                .map(GrantedAuthority::getAuthority)
                .orElse("ROLE_UNKNOWN");
        Integer userId = userRepository.findByUsername(username)
                .map(User::getUserId)
                .orElse(null);
        return new LogContext(userId, username, role);
    }

    private record LogContext(Integer userId, String username, String role) {}
}
