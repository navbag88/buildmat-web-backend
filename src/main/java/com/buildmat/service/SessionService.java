package com.buildmat.service;

import com.buildmat.model.UserSessionEntity;
import com.buildmat.repository.UserSessionRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

    private final UserSessionRepository sessionRepo;

    @Value("${session.expiry.seconds:86400}")
    private long sessionExpirySeconds;

    @Value("${session.cookie.secure:true}")
    private boolean cookieSecure;

    public static final String SESSION_COOKIE = "BUILDMAT_SESSION";

    /**
     * Creates a new server-side session, stores it in DB, and sets an HTTP-only cookie.
     * Each browser that calls this gets its own unique session bound to that browser's cookies.
     */
    @Transactional
    public UserSessionEntity createSession(Long userId, String username, String role,
                                           HttpServletRequest request, HttpServletResponse response) {
        String token = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        UserSessionEntity session = UserSessionEntity.builder()
            .sessionToken(token)
            .userId(userId)
            .username(username)
            .role(role)
            .ipAddress(getClientIp(request))
            .userAgent(truncate(request.getHeader("User-Agent"), 512))
            .createdAt(now)
            .expiresAt(now.plusSeconds(sessionExpirySeconds))
            .active(true)
            .build();

        sessionRepo.save(session);
        addSessionCookie(token, response);
        log.info("Session created: username='{}' role={} ip={} expiresAt={}",
            username, role, session.getIpAddress(), session.getExpiresAt());
        return session;
    }

    /**
     * Validates the session cookie from the incoming request against the DB.
     * Returns empty if: cookie missing, session not found, session expired, or session inactive.
     */
    public Optional<UserSessionEntity> validateSession(HttpServletRequest request) {
        String token = extractTokenFromCookies(request);
        if (token == null || token.isBlank()) return Optional.empty();

        return sessionRepo.findBySessionTokenAndActiveTrue(token)
            .filter(s -> s.getExpiresAt().isAfter(LocalDateTime.now()));
    }

    /**
     * Invalidates the current session in the DB and clears the browser cookie.
     */
    @Transactional
    public void invalidateSession(HttpServletRequest request, HttpServletResponse response) {
        String token = extractTokenFromCookies(request);
        if (token != null && !token.isBlank()) {
            sessionRepo.findBySessionTokenAndActiveTrue(token).ifPresent(s ->
                log.info("Session invalidated: username='{}' sessionId={}", s.getUsername(), token.substring(0, 8) + "..."));
            sessionRepo.deactivateByToken(token);
        }
        clearSessionCookie(response);
    }

    /**
     * Lists all active sessions for a user (useful for admin session management).
     */
    public List<UserSessionEntity> getActiveSessions(Long userId) {
        return sessionRepo.findByUserIdAndActiveTrue(userId);
    }

    /**
     * Force-invalidates all sessions for a user (e.g., password change, account lock).
     */
    @Transactional
    public void invalidateAllSessionsForUser(Long userId) {
        sessionRepo.deactivateAllByUserId(userId);
    }

    /** Cleanup expired sessions - runs every hour */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanExpiredSessions() {
        sessionRepo.deleteExpiredSessions(LocalDateTime.now());
        log.debug("Expired session cleanup ran");
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private void addSessionCookie(String token, HttpServletResponse response) {
        Cookie cookie = new Cookie(SESSION_COOKIE, token);
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath("/");
        cookie.setMaxAge((int) sessionExpirySeconds);
        // SameSite=Strict: cookie is NOT sent with cross-site requests
        response.addHeader("Set-Cookie",
            String.format("%s=%s; Path=/; HttpOnly; %sSameSite=Strict; Max-Age=%d",
                SESSION_COOKIE, token,
                cookieSecure ? "Secure; " : "",
                sessionExpirySeconds));
    }

    private void clearSessionCookie(HttpServletResponse response) {
        response.addHeader("Set-Cookie",
            String.format("%s=; Path=/; HttpOnly; %sSameSite=Strict; Max-Age=0",
                SESSION_COOKIE,
                cookieSecure ? "Secure; " : ""));
    }

    private String extractTokenFromCookies(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
            .filter(c -> SESSION_COOKIE.equals(c.getName()))
            .map(Cookie::getValue)
            .findFirst()
            .orElse(null);
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) return realIp.trim();
        return request.getRemoteAddr();
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
