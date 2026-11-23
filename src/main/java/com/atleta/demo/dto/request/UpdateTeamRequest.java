package com.atleta.demo.dto.request;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;

public record UpdateTeamRequest(String nombre) {
    public UpdateTeamRequest {
        if (nombre == null) {
            throw new IllegalArgumentException("El nombre no puede ser nulo");
        }
        nombre = trimWhitespace(nombre);
        if (nombre.isEmpty()) {
            throw new IllegalArgumentException("El nombre no puede estar vacío");
        }
        if (nombre.length() > 100) {
            throw new IllegalArgumentException("El nombre admite hasta 100 caracteres");
        }
        if (nombre.codePoints().anyMatch(value ->
                Character.isISOControl(value) || Character.getType(value) == Character.SURROGATE)) {
            throw new IllegalArgumentException("El nombre debe ser texto Unicode válido y sin caracteres de control");
        }
    }

    private static String trimWhitespace(String name) {
        int start = 0;
        int end = name.length();
        while (start < end && isWhitespace(name.charAt(start))) {
            start++;
        }
        while (end > start && isWhitespace(name.charAt(end - 1))) {
            end--;
        }
        return name.substring(start, end);
    }

    private static boolean isWhitespace(char value) {
        return Character.isWhitespace(value) || Character.isSpaceChar(value);
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static UpdateTeamRequest fromJson(JsonNode body) {
        if (body == null || !body.isObject()) {
            throw new IllegalArgumentException("Se requiere un objeto JSON");
        }
        if (!body.has("nombre")) {
            throw new IllegalArgumentException("Falta el campo nombre");
        }
        if (body.size() != 1) {
            throw new IllegalArgumentException("Solo se admite el campo nombre");
        }
        JsonNode name = body.get("nombre");
        if (!name.isTextual() && !name.isNull()) {
            throw new IllegalArgumentException("El nombre debe ser texto");
        }
        return new UpdateTeamRequest(name.textValue());
    }
}
