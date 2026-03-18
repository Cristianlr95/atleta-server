package com.atleta.demo.exception;

/**
 * Excepción específica para errores de validación de datos de jugadores.
 * Se lanza cuando los datos proporcionados para un jugador son inválidos, incompletos,
 * o no cumplen con las reglas de negocio del sistema de calificación.
 * 
 * Ejemplos de casos que generan esta excepción:
 * - Datos de rendimiento con valores negativos inválidos
 * - Información de jugador faltante o inconsistente
 * - Violaciones de reglas de negocio específicas (ej: múltiples MVPs)
 * - Datos de rol o prioridad inválidos
 * 
 * Implementa el requerimiento 9.4: Validación robusta de datos de entrada.
 */
public class InvalidPlayerDataException extends RatingCalculationException {

    private final String playerProfileId;
    private final String fieldName;
    private final Object invalidValue;

    /**
     * Construye una nueva excepción de datos de jugador inválidos con el mensaje especificado.
     * 
     * @param message el mensaje de detalle que describe la causa del error
     */
    public InvalidPlayerDataException(String message) {
        super(message);
        this.playerProfileId = null;
        this.fieldName = null;
        this.invalidValue = null;
    }

    /**
     * Construye una nueva excepción de datos de jugador inválidos con información detallada.
     * 
     * @param message el mensaje de detalle que describe la causa del error
     * @param playerProfileId el ID del jugador que tiene datos inválidos (puede ser null)
     * @param fieldName el nombre del campo que contiene datos inválidos (puede ser null)
     * @param invalidValue el valor inválido que causó el error (puede ser null)
     */
    public InvalidPlayerDataException(String message, String playerProfileId, String fieldName, Object invalidValue) {
        super(message);
        this.playerProfileId = playerProfileId;
        this.fieldName = fieldName;
        this.invalidValue = invalidValue;
    }

    /**
     * Construye una nueva excepción de datos de jugador inválidos con mensaje y causa.
     * 
     * @param message el mensaje de detalle que describe la causa del error
     * @param cause la causa raíz del error
     */
    public InvalidPlayerDataException(String message, Throwable cause) {
        super(message, cause);
        this.playerProfileId = null;
        this.fieldName = null;
        this.invalidValue = null;
    }

    /**
     * Obtiene el ID del jugador que tiene datos inválidos.
     * 
     * @return el ID del jugador, o null si no está disponible
     */
    public String getPlayerProfileId() {
        return playerProfileId;
    }

    /**
     * Obtiene el nombre del campo que contiene datos inválidos.
     * 
     * @return el nombre del campo, o null si no está disponible
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * Obtiene el valor inválido que causó el error.
     * 
     * @return el valor inválido, o null si no está disponible
     */
    public Object getInvalidValue() {
        return invalidValue;
    }

    /**
     * Crea un mensaje de error detallado incluyendo información del contexto.
     * 
     * @return mensaje de error con contexto adicional si está disponible
     */
    @Override
    public String getMessage() {
        StringBuilder message = new StringBuilder(super.getMessage());
        
        if (playerProfileId != null) {
            message.append(" [Jugador: ").append(playerProfileId).append("]");
        }
        
        if (fieldName != null) {
            message.append(" [Campo: ").append(fieldName).append("]");
        }
        
        if (invalidValue != null) {
            message.append(" [Valor inválido: ").append(invalidValue).append("]");
        }
        
        return message.toString();
    }
}