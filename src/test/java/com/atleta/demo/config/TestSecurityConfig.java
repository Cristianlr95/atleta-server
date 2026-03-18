package com.atleta.demo.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Configuración de seguridad para pruebas.
 * Proporciona los beans necesarios para que las pruebas puedan ejecutarse correctamente.
 */
@TestConfiguration
public class TestSecurityConfig {

    /**
     * Proporciona un PasswordEncoder para las pruebas.
     * Utiliza BCryptPasswordEncoder como implementación por defecto.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}