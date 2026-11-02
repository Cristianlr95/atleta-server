package com.atleta.demo.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
@Profile("beta")
public class BetaRegistrationFilter extends OncePerRequestFilter {
    private final String invitationCode;

    public BetaRegistrationFilter(@Value("${app.beta.invitation-code:}") String invitationCode) {
        this.invitationCode = invitationCode;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if ("POST".equals(request.getMethod())) {
            String path = request.getServletPath();
            if ("/api/v1/athletes/auth/google".equals(path)) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            if ("/api/v1/athletes/register".equals(path)) {
                if (invitationCode.length() < 20) {
                    response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                    return;
                }
                String supplied = request.getHeader("X-Beta-Invite");
                if (supplied == null || !MessageDigest.isEqual(invitationCode.getBytes(StandardCharsets.UTF_8),
                        supplied.getBytes(StandardCharsets.UTF_8))) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
            }
        }
        chain.doFilter(request, response);
    }
}
