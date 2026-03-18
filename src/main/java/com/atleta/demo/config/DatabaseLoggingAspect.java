package com.atleta.demo.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Aspecto para logging contextual de operaciones de base de datos
 * Solo se activa en ambientes donde el logging de performance está habilitado
 */
@Aspect
@Component
@ConditionalOnProperty(name = "logging.database.performance.enabled", havingValue = "true", matchIfMissing = false)
public class DatabaseLoggingAspect {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseLoggingAspect.class);

    /**
     * Intercepta llamadas a repositorios para logging de performance
     */
    @Around("execution(* com.atleta.demo.repository.*.*(..))")
    public Object logRepositoryMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        
        // Agregar contexto específico de base de datos
        MDC.put("dbOperation", methodName);
        MDC.put("repository", className);
        
        long startTime = System.currentTimeMillis();
        
        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Log de performance solo en staging
            if (executionTime > 100) { // Log si toma más de 100ms
                logger.info("Database operation completed - Method: {}.{}, Duration: {}ms", 
                           className, methodName, executionTime);
            }
            
            return result;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("Database operation failed - Method: {}.{}, Duration: {}ms, Error: {}", 
                        className, methodName, executionTime, e.getMessage());
            throw e;
        } finally {
            // Limpiar contexto específico de DB
            MDC.remove("dbOperation");
            MDC.remove("repository");
        }
    }

    /**
     * Intercepta llamadas a servicios para logging de transacciones
     */
    @Around("execution(* com.atleta.demo.service.*.*(..))")
    public Object logServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        
        // Agregar contexto de servicio
        MDC.put("serviceOperation", methodName);
        MDC.put("service", className);
        
        long startTime = System.currentTimeMillis();
        
        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;
            
            // Log de transacciones de negocio
            if (executionTime > 500) { // Log si toma más de 500ms
                logger.info("Service operation completed - Method: {}.{}, Duration: {}ms", 
                           className, methodName, executionTime);
            }
            
            return result;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("Service operation failed - Method: {}.{}, Duration: {}ms, Error: {}", 
                        className, methodName, executionTime, e.getMessage());
            throw e;
        } finally {
            // Limpiar contexto de servicio
            MDC.remove("serviceOperation");
            MDC.remove("service");
        }
    }
}