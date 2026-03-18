package com.atleta.demo.dto.request;

import com.atleta.demo.enums.GenderType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO para la creación de un nuevo atleta.
 * Contiene las validaciones de negocio necesarias para el registro.
 */
public class CreateAthleteRequest {

    /**
     * Email único del atleta - usado para autenticación
     */
    @Email(message = "El email debe tener un formato válido")
    @NotBlank(message = "El email es obligatorio")
    private String email;

    /**
     * Contraseña del atleta (será hasheada antes de almacenar)
     */
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 100, message = "La contraseña debe tener entre 8 y 100 caracteres")
    private String password;

    /**
     * Nombre completo del atleta
     */
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    private String nombre;

    @NotNull(message = "El genero es obligatorio")
    private GenderType genero;

    // Constructors
    public CreateAthleteRequest() {
    }

    public CreateAthleteRequest(String email, String password, String nombre, GenderType genero) {
        this.email = email;
        this.password = password;
        this.nombre = nombre;
        this.genero = genero;
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

    @Override
    public String toString() {
        return "CreateAthleteRequest{" +
                "email='" + email + '\'' +
                ", nombre='" + nombre + '\'' +
                '}';
    }
}
