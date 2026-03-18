package com.atleta.demo.exception;

/**
 * Excepción específica para casos donde un jugador requerido no se encuentra en el sistema.
 * Se lanza cuando se intenta realizar operaciones sobre un jugador que no existe
 * o cuando las referencias a jugadores son inválidas.
 * 
 * Esta excepción ayuda a distinguir entre errores de datos inválidos y casos donde
 * simplemente el recurso solicitado no existe.
 * 
 * Implementa el requerimiento 9.4: Validación robusta de datos de entrada.
 */
public class PlayerNotFoundException extends RatingCalculationException {

    private final String playerProfileId;
    private final String searchCriteria;

    /**
     * Construye una nueva excepción de jugador no encontrado con el mensaje especificado.
     * 
     * @param message el mensaje de detalle que describe la causa del error
     */
    public PlayerNotFoundException(String message) {
        super(message);
        this.playerProfileId = null;
        this.searchCriteria = null;
    }

    /**
     * Construye una nueva excepción de jugador no encontrado con ID específico.
     * 
     * @param message el mensaje de detalle que describe la causa del error
     * @param playerProfileId el ID del jugador que no se pudo encontrar
     */
    public PlayerNotFoundException(String message, String playerProfileId) {
        super(message);
        this.playerProfileId = playerProfileId;
        this.searchCriteria = null;
    }

    /**
     * Construye una nueva excepción de jugador no encontrado con criterios de búsqueda.
     * 
     * @param message el mensaje de detalle que describe la causa del error
     * @param playerProfileId el ID del jugador que no se pudo encontrar
     * @param searchCriteria los criterios de búsqueda utilizados
     */
    public PlayerNotFoundException(String message, String playerProfileId, String searchCriteria) {
        super(message);
        this.playerProfileId = playerProfileId;
        this.searchCriteria = searchCriteria;
    }

    /**
     * Construye una nueva excepción de jugador no encontrado con mensaje y causa.
     * 
     * @param message el mensaje de detalle que describe la causa del error
     * @param cause la causa raíz del error
     */
    public PlayerNotFoundException(String message, Throwable cause) {
        super(message, cause);
        this.playerProfileId = null;
        this.searchCriteria = null;
    }

    /**
     * Obtiene el ID del jugador que no se pudo encontrar.
     * 
     * @return el ID del jugador, o null si no está disponible
     */
    public String getPlayerProfileId() {
        return playerProfileId;
    }

    /**
     * Obtiene los criterios de búsqueda utilizados.
     * 
     * @return los criterios de búsqueda, o null si no están disponibles
     */
    public String getSearchCriteria() {
        return searchCriteria;
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
            message.append(" [ID del jugador: ").append(playerProfileId).append("]");
        }
        
        if (searchCriteria != null) {
            message.append(" [Criterios de búsqueda: ").append(searchCriteria).append("]");
        }
        
        return message.toString();
    }
}