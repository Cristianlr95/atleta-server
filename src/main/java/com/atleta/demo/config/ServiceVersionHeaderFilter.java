package com.atleta.demo.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Publishes the deployed service contract version for diagnostics and clients. */
@Component
public class ServiceVersionHeaderFilter extends OncePerRequestFilter {

    private final String serviceVersion;

    public ServiceVersionHeaderFilter(
            @Value("${atleta.service.version:2026.07}") String serviceVersion
    ) {
        this.serviceVersion = serviceVersion;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        response.setHeader("X-Atleta-Service-Version", serviceVersion);
        filterChain.doFilter(request, response);
    }
}
