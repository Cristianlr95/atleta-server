package com.atleta.demo.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.atleta.demo.enums.GenderType;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entidad que representa la identidad global de un atleta en el sistema.
 * Cada atleta tiene un UUID único como identificador principal y un email único.
 * Esta es la entidad raíz para toda la información relacionada con un atleta.
 */
@Entity
@Table(name = "athletes")
public class Athlete {

    /**
     * Identificador único UUID del atleta (Primary Key)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "atleta_uuid", nullable = false, updatable = false)
    private UUID atletaUuid;

    /**
     * Email único del atleta - usado para autenticación
     */
    @Column(name = "email", unique = true, nullable = false)
    @Email(message = "El email debe tener un formato válido")
    @NotBlank(message = "El email es obligatorio")
    private String email;

    /**
     * Hash de la contraseña del atleta (nullable para usuarios OAuth)
     */
    @Column(name = "password_hash")
    private String passwordHash;

    /**
     * Nombre completo del atleta
     */
    @Column(name = "nombre", nullable = false)
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    private String nombre;

    /**
     * Proveedor de autenticación: LOCAL o GOOGLE
     */
    @Column(name = "auth_provider", nullable = false)
    @NotBlank(message = "El proveedor de autenticación es obligatorio")
    private String authProvider = "LOCAL";

    /**
     * ID único de Google (sub claim del token)
     */
    @Column(name = "google_id", unique = true)
    private String googleId;

    /**
     * URL de la foto de perfil de Google
     */
    @Column(name = "picture_url", length = 500)
    private String pictureUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "genero")
    private GenderType genero;

    /**
     * Timestamp de creación del registro
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Timestamp de última actualización
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * Versión para optimistic locking
     */
    @Version
    @Column(name = "version")
    private Long version;

    /**
     * Relación uno-a-uno con PlayerProfile
     */
    @OneToOne(mappedBy = "athlete", fetch = FetchType.LAZY)
    private PlayerProfile playerProfile;

    // Constructors
    public Athlete() {
        // Constructor por defecto para JPA
    }

    public Athlete(String email, String passwordHash, String nombre) {
        this(email, passwordHash, nombre, null);
    }

    public Athlete(String email, String passwordHash, String nombre, GenderType genero) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.nombre = nombre;
        this.genero = genero;
    }

    // Getters and Setters
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

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public PlayerProfile getPlayerProfile() {
        return playerProfile;
    }

    public void setPlayerProfile(PlayerProfile playerProfile) {
        this.playerProfile = playerProfile;
    }

    public String getAuthProvider() {
        return authProvider;
    }

    public void setAuthProvider(String authProvider) {
        this.authProvider = authProvider;
    }

    public String getGoogleId() {
        return googleId;
    }

    public void setGoogleId(String googleId) {
        this.googleId = googleId;
    }

    public String getPictureUrl() {
        return pictureUrl;
    }

    public void setPictureUrl(String pictureUrl) {
        this.pictureUrl = pictureUrl;
    }

    public GenderType getGenero() {
        return genero;
    }

    public void setGenero(GenderType genero) {
        this.genero = genero;
    }

    // Utility methods
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Athlete athlete = (Athlete) obj;
        return atletaUuid != null && atletaUuid.equals(athlete.atletaUuid);
    }

    @Override
    public int hashCode() {
        return atletaUuid != null ? atletaUuid.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Athlete{" +
                "atletaUuid=" + atletaUuid +
                ", email='" + email + '\'' +
                ", nombre='" + nombre + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
