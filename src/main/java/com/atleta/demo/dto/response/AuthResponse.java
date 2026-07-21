package com.atleta.demo.dto.response;

import com.atleta.demo.enums.GenderType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de respuesta para autenticación exitosa.
 * Incluye información del atleta y token JWT.
 */
public class AuthResponse {
    
    private UUID atletaUuid;
    private String email;
    private String nombre;
    private GenderType genero;
    private String authProvider; // "LOCAL" o "GOOGLE"
    private String accessToken;
    private String refreshToken;
    private LocalDateTime authenticatedAt;
    
    public AuthResponse() {
    }
    
    public AuthResponse(UUID atletaUuid, String email, String nombre, GenderType genero,
                       String authProvider, String accessToken) {
        this(atletaUuid, email, nombre, genero, authProvider, accessToken, null);
    }

    public AuthResponse(UUID atletaUuid, String email, String nombre, GenderType genero,
                       String authProvider, String accessToken, String refreshToken) {
        this.atletaUuid = atletaUuid;
        this.email = email;
        this.nombre = nombre;
        this.genero = genero;
        this.authProvider = authProvider;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.authenticatedAt = LocalDateTime.now();
    }
    
    // Getters y Setters
    
    public UUID getAtletaUuid() {
        return atletaUuid;
    }
    
    public void setAtletaUuid(UUID atletaUuid) {
        this.atletaUuid = atletaUuid;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getNombre() {
        return nombre;
    }
    
    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public GenderType getGenero() {
        return genero;
    }

    public void setGenero(GenderType genero) {
        this.genero = genero;
    }
    
    public String getAuthProvider() {
        return authProvider;
    }
    
    public void setAuthProvider(String authProvider) {
        this.authProvider = authProvider;
    }
    
    public String getAccessToken() {
        return accessToken;
    }
    
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
    
    public LocalDateTime getAuthenticatedAt() {
        return authenticatedAt;
    }
    
    public void setAuthenticatedAt(LocalDateTime authenticatedAt) {
        this.authenticatedAt = authenticatedAt;
    }
}
