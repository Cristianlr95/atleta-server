package com.atleta.demo.dto.request;

import com.atleta.demo.enums.GenderType;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * DTO para la creación de un perfil de jugador.
 * Se asocia a un atleta existente y permite definir un alias opcional.
 */
public class CreatePlayerProfileRequest {

    /**
     * UUID del atleta al que se asociará el perfil
     */
    private UUID atletaUuid;

    /**
     * Alias del jugador en el contexto de fútbol (opcional)
     */
    @Size(max = 50, message = "El alias no puede exceder 50 caracteres")
    private String alias;

    private GenderType genero;

    // Constructors
    public CreatePlayerProfileRequest() {
    }

    public CreatePlayerProfileRequest(UUID atletaUuid, String alias, GenderType genero) {
        this.atletaUuid = atletaUuid;
        this.alias = alias;
        this.genero = genero;
    }

    // Getters and Setters
    public UUID getAtletaUuid() {
        return atletaUuid;
    }

    public void setAtletaUuid(UUID atletaUuid) {
        this.atletaUuid = atletaUuid;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public GenderType getGenero() {
        return genero;
    }

    public void setGenero(GenderType genero) {
        this.genero = genero;
    }

    @Override
    public String toString() {
        return "CreatePlayerProfileRequest{" +
                "atletaUuid=" + atletaUuid +
                ", alias='" + alias + '\'' +
                '}';
    }
}
