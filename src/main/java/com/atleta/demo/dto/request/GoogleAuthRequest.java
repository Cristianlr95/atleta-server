package com.atleta.demo.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO para autenticación con Google OAuth2.
 * Contiene el token de ID de Google que debe ser validado.
 */
public class GoogleAuthRequest {
    
    @NotBlank(message = "El token de Google es obligatorio")
    private String idToken;
    
    public GoogleAuthRequest() {
    }
    
    public GoogleAuthRequest(String idToken) {
        this.idToken = idToken;
    }
    
    public String getIdToken() {
        return idToken;
    }
    
    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }
}
