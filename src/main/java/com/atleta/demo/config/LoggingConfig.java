package com.atleta.demo.config;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;

/**
 * Configuración de logging contextual para trazabilidad
 * Implementa MDC (Mapped Diagnostic Context) para incluir información
 * de usuario y transacción en todos los logs
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

    /**
     * Interceptor que configura el contexto de logging para cada request
     */
    public static class LoggingInterceptor implements HandlerInterceptor {

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
            // Generar ID único para la transacción
            String transactionId = UUID.randomUUID().toString().substring(0, 8);
            MDC.put("transactionId", transactionId);
            
            // Extraer información del usuario si está disponible
            String userId = extractUserId(request);
            if (userId != null) {
                MDC.put("userId", userId);
            } else {
                MDC.put("userId", "anonymous");
            }
            
            // Información adicional del request
            MDC.put("requestMethod", request.getMethod());
            MDC.put("requestURI", request.getRequestURI());
            MDC.put("remoteAddr", getClientIpAddress(request));
            
            return true;
        }

        @Override
        public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                                  Object handler, Exception ex) {
            // Limpiar el contexto MDC después del request
            MDC.clear();
        }

        /**
         * Extrae el ID del usuario del request
         * Busca en headers de autenticación o JWT
         */
        private String extractUserId(HttpServletRequest request) {
            // Buscar en header Authorization
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                // En una implementación real, aquí se decodificaría el JWT
                // Por ahora retornamos un placeholder
                return "user-from-jwt";
            }
            
            // Buscar en header personalizado
            String userIdHeader = request.getHeader("X-User-ID");
            if (userIdHeader != null) {
                return userIdHeader;
            }
            
            // Buscar en sesión
            if (request.getSession(false) != null) {
                Object userId = request.getSession().getAttribute("userId");
                if (userId != null) {
                    return userId.toString();
                }
            }
            
            return null;
        }

        /**
         * Obtiene la dirección IP real del cliente considerando proxies
         */
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
