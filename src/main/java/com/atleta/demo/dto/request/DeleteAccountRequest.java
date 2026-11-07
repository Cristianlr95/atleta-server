package com.atleta.demo.dto.request;

import jakarta.validation.constraints.NotBlank;

/** Confirma la eliminación irreversible de la identidad de acceso del atleta. */
public class DeleteAccountRequest {

    @NotBlank(message = "La confirmación es obligatoria")
    private String confirmation;

    private String currentPassword;

    public String getConfirmation() {
        return confirmation;
    }

    public void setConfirmation(String confirmation) {
        this.confirmation = confirmation;
    }

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }
}
