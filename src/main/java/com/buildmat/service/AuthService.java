package com.buildmat.service;

import com.buildmat.model.*;
import com.buildmat.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Service @RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepo;
    private final PasswordEncoder encoder;
    private final SessionService sessionService;

    /**
     * Authenticates credentials, creates a server-side session, and sets an HTTP-only
     * session cookie on the response.  Each call produces a new independent session, so
     * a second browser logging in gets its own separate session.
     */
    public ResponseEntity<?> login(String username, String password,
                                   HttpServletRequest request, HttpServletResponse response) {
        String ip = getClientIp(request);
        String cleanUsername = username.trim().toLowerCase();
        log.info("Login attempt: username='{}' ip={}", cleanUsername, ip);

        Optional<UserEntity> opt = userRepo.findByUsernameAndActiveTrue(cleanUsername);
        if (opt.isEmpty() || !encoder.matches(password, opt.get().getPasswordHash())) {
            log.warn("Login FAILED: username='{}' ip={} reason={}",
                cleanUsername, ip, opt.isEmpty() ? "user-not-found" : "wrong-password");
            return ResponseEntity.status(401).body(Map.of("error", "Invalid username or password"));
        }

        UserEntity u = opt.get();
        sessionService.createSession(u.getId(), u.getUsername(), u.getRole(), request, response);
        log.info("Login SUCCESS: username='{}' role={} ip={}", u.getUsername(), u.getRole(), ip);

        // Return user metadata (NOT a token — auth is now cookie-based)
        return ResponseEntity.ok(Map.of(
            "username", u.getUsername(),
            "fullName", u.getFullName(),
            "role", u.getRole(),
            "id", u.getId()
        ));
    }

    /**
     * Invalidates the current session and clears the browser cookie.
     */
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("Logout: username='{}'", username);
        sessionService.invalidateSession(request, response);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    public ResponseEntity<?> me() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepo.findByUsernameAndActiveTrue(username)
            .map(u -> ResponseEntity.ok(Map.of(
                "username", u.getUsername(), "fullName", u.getFullName(),
                "role", u.getRole(), "id", u.getId()
            )))
            .orElse(ResponseEntity.status(401).build());
    }

    @jakarta.annotation.PostConstruct
    public void seedAdmin() {
        if (!userRepo.existsByUsername("superadmin")) {
            userRepo.save(UserEntity.builder()
                .username("superadmin").fullName("Super Administrator")
                .role("SUPER_ADMIN").active(true)
                .passwordHash(encoder.encode("superadmin123")).build());
            log.info("Default user seeded: username='superadmin' role=SUPER_ADMIN");
        }
        if (!userRepo.existsByUsername("admin")) {
            userRepo.save(UserEntity.builder()
                .username("admin").fullName("Administrator").role("ADMIN").active(true)
                .passwordHash(encoder.encode("admin123")).build());
            log.info("Default user seeded: username='admin' role=ADMIN");
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) return realIp.trim();
        return request.getRemoteAddr();
    }
}
