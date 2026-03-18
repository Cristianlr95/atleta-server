package com.atleta.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO para el cambio de contraseña de un atleta.
 */
public class ChangePasswordRequest {

    /**
     * Contraseña actual del atleta
     */
    @NotBlank(message = "La contraseña actual es obligatoria")
    private String currentPassword;

    /**
     * Nueva contraseña del atleta
     */
    @NotBlank(message = "La nueva contraseña es obligatoria")
    @Size(min = 8, max = 100, message = "La nueva contraseña debe tener entre 8 y 100 caracteres")
    private String newPassword;

    // Constructors
    public ChangePasswordRequest() {
    }

    public ChangePasswordRequest(String currentPassword, String newPassword) {
        this.currentPassword = currentPassword;
        this.newPassword = newPassword;
    }

    // Getters and Setters
    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    @Override
    public String toString() {
        return "ChangePasswordRequest{" +
                "currentPassword='[PROTECTED]'" +
                ", newPassword='[PROTECTED]'" +
                '}';
    }
}