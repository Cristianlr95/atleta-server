package com.atleta.demo.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Profile("beta")
public class BetaManualWritesFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getServletPath();
        String method = request.getMethod();

        if (("POST".equals(method) && (
                "/api/v1/ratings/update".equals(path)
                || "/api/v1/ratings/update-rotative-goalkeeper".equals(path)))
                || ("PUT".equals(method) && (
                "/api/v1/player-profiles/trust-score".equals(path)
                || path.matches("/api/v1/player-profiles/[^/]+/positions/[^/]+/experience")))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
