package com.atleta.demo.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.UUID;

/**
 * Configuracion de logging contextual para trazabilidad.
 */
@Configuration
public class LoggingConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoggingInterceptor());
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
            .addResourceLocations("file:uploads/");
    }

    public static class LoggingInterceptor implements HandlerInterceptor {

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            String transactionId = UUID.randomUUID().toString().substring(0, 8);
            MDC.put("transactionId", transactionId);

            String userId = extractUserId(request);
            MDC.put("userId", userId != null ? userId : "anonymous");

            MDC.put("requestMethod", request.getMethod());
            MDC.put("requestURI", request.getRequestURI());
            MDC.put("remoteAddr", getClientIpAddress(request));

            return true;
        }

        @Override
        public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                    Object handler, Exception ex) {
            MDC.clear();
        }

        private String extractUserId(HttpServletRequest request) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                Object principal = authentication.getPrincipal();
                if (principal instanceof Jwt jwt) {
                    return jwt.getSubject();
                }
                if (principal instanceof String principalValue && !"anonymousUser".equals(principalValue)) {
                    return principalValue;
                }
            }

            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                return "bearer-token-present";
            }

            String userIdHeader = request.getHeader("X-User-ID");
            if (userIdHeader != null) {
                return userIdHeader;
            }

            if (request.getSession(false) != null) {
                Object userId = request.getSession().getAttribute("userId");
                if (userId != null) {
                    return userId.toString();
                }
            }

            return null;
        }

        private String getClientIpAddress(HttpServletRequest request) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                return xForwardedFor.split(",")[0].trim();
            }

            String xRealIp = request.getHeader("X-Real-IP");
            if (xRealIp != null && !xRealIp.isEmpty()) {
                return xRealIp;
            }

            return request.getRemoteAddr();
        }
    }
}
