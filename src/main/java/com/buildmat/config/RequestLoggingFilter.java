package com.buildmat.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Logs every API request: method, URI, authenticated user, HTTP status, and elapsed time.
 * Static assets are skipped to keep the log readable.
 */
@Slf4j
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        try {
            chain.doFilter(request, response);
        } finally {
            long ms   = System.currentTimeMillis() - start;
            String uri  = request.getRequestURI();
            String query = request.getQueryString();
            String user  = request.getRemoteUser();

            // API calls → INFO; everything else (SPA routes, health) → DEBUG
            if (uri.startsWith("/api/")) {
                log.info("[HTTP] {} {}{} | user={} | status={} | {}ms",
                    request.getMethod(),
                    uri,
                    query != null ? "?" + query : "",
                    user != null ? user : "anonymous",
                    response.getStatus(),
                    ms);
            } else {
                log.debug("[HTTP] {} {} | status={} | {}ms",
                    request.getMethod(), uri, response.getStatus(), ms);
            }
        }
    }

    /** Skip static assets — they are not useful in the log. */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/assets/")
            || uri.endsWith(".js")
            || uri.endsWith(".css")
            || uri.endsWith(".ico")
            || uri.endsWith(".png")
            || uri.endsWith(".jpg")
            || uri.endsWith(".jpeg")
            || uri.endsWith(".svg")
            || uri.endsWith(".woff")
            || uri.endsWith(".woff2")
            || uri.endsWith(".map");
    }
}
