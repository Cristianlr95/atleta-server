package com.atleta.demo.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO para la autenticación de atletas.
 * Contiene las credenciales necesarias para el login.
 */
public class LoginRequest {

    /**
     * Email del atleta
     */
    @Email(message = "El email debe tener un formato válido")
    @NotBlank(message = "El email es obligatorio")
    private String email;

    /**
     * Contraseña del atleta
     */
    @NotBlank(message = "La contraseña es obligatoria")
    private String password;

    // Constructors
    public LoginRequest() {
    }

    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    // Getters and Setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "LoginRequest{" +
                "email='" + email + '\'' +
                '}';
    }
}