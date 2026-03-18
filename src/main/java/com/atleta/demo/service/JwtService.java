package com.atleta.demo.service;

import com.atleta.demo.entity.Athlete;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.UUID;

/**
 * Servicio simple para generar tokens JWT.
 * En producción, usar una librería como jjwt o spring-security-oauth2-jose.
 */
@Service
public class JwtService {

    /**
     * Genera un token JWT simple para el atleta.
     * NOTA: Esta es una implementación básica para desarrollo.
     * En producción, usar una librería JWT completa con firma y validación.
     * 
     * @param athlete Atleta autenticado
     * @return Token JWT
     */
    public String generateToken(Athlete athlete) {
        // Implementación simple: Base64(atletaUuid:email:authProvider)
        // En producción, usar jjwt o spring-security-oauth2-jose
        String payload = athlete.getAtletaUuid() + ":" + 
                        athlete.getEmail() + ":" + 
                        athlete.getAuthProvider();
        
        return Base64.getEncoder().encodeToString(payload.getBytes());
    }

    /**
     * Valida y decodifica un token JWT.
     * 
     * @param token Token JWT
     * @return UUID del atleta
     */
    public UUID validateToken(String token) {
        try {
            String decoded = new String(Base64.getDecoder().decode(token));
            String[] parts = decoded.split(":");
            return UUID.fromString(parts[0]);
        } catch (Exception e) {
            throw new IllegalArgumentException("Token inválido");
        }
    }
}
