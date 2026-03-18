package com.atleta.demo.exception;

/**
 * Excepción específica para casos donde un partido requerido no se encuentra en el sistema.
 * Se lanza cuando se intenta realizar operaciones sobre un partido que no existe
 * o cuando las referencias a partidos son inválidas.
 * 
 * Esta excepción es importante para el sistema de calificación ya que todas las
 * actualizaciones de calificación deben estar asociadas a un partido válido.
 * 
 * Implementa el requerimiento 9.4: Validación robusta de datos de entrada.
 */
public class MatchNotFoundException extends RatingCalculationException {

    private final Long matchId;
    private final String searchCriteria;

    /**
     * Construye una nueva excepción de partido no encontrado con el mensaje especificado.
     * 
     * @param message el mensaje de detalle que describe la causa del error
     */
    public MatchNotFoundException(String message) {
        super(message);
        this.matchId = null;
        this.searchCriteria = null;
    }

    /**
     * Construye una nueva excepción de partido no encontrado con ID específico.
     * 
     * @param message el mensaje de detalle que describe la causa del error
     * @param matchId el ID del partido que no se pudo encontrar
     */
    public MatchNotFoundException(String message, Long matchId) {
        super(message);
        this.matchId = matchId;
        this.searchCriteria = null;
    }

    /**
     * Construye una nueva excepción de partido no encontrado con criterios de búsqueda.
     * 
     * @param message el mensaje de detalle que describe la causa del error
     * @param matchId el ID del partido que no se pudo encontrar
     * @param searchCriteria los criterios de búsqueda utilizados
     */
    public MatchNotFoundException(String message, Long matchId, String searchCriteria) {
        super(message);
        this.matchId = matchId;
        this.searchCriteria = searchCriteria;
    }

    /**
     * Construye una nueva excepción de partido no encontrado con mensaje y causa.
     * 
     * @param message el mensaje de detalle que describe la causa del error
     * @param cause la causa raíz del error
     */
    public MatchNotFoundException(String message, Throwable cause) {
        super(message, cause);
        this.matchId = null;
        this.searchCriteria = null;
    }

    /**
     * Obtiene el ID del partido que no se pudo encontrar.
     * 
     * @return el ID del partido, o null si no está disponible
     */
    public Long getMatchId() {
        return matchId;
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
        
        if (matchId != null) {
            message.append(" [ID del partido: ").append(matchId).append("]");
        }
        
        if (searchCriteria != null) {
            message.append(" [Criterios de búsqueda: ").append(searchCriteria).append("]");
        }
        
        return message.toString();
    }
}