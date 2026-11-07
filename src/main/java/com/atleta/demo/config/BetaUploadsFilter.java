package com.atleta.demo.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Avoid accepting images that Render's ephemeral filesystem would discard. */
@Component
@ConditionalOnProperty(name = "app.uploads.enabled", havingValue = "false")
public class BetaUploadsFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {
        String path = request.getServletPath();
        if ("POST".equals(request.getMethod()) && (
                "/api/v1/teams/logo".equals(path)
                || path.matches("/api/v1/player-profiles/[^/]+/picture"))) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.setContentType("application/json");
            response.getWriter().write("{\"message\":\"La carga de imagenes no esta disponible en la beta.\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
