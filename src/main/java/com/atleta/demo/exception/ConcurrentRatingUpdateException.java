package com.atleta.demo.exception;

/**
 * Excepción específica para errores de concurrencia en actualizaciones de calificación.
 * Se lanza cuando ocurren conflictos durante actualizaciones simultáneas de calificaciones
 * de jugadores, como violaciones de optimistic locking o condiciones de carrera.
 * 
 * Esta excepción indica que la operación debe reintentarse, ya que el estado de los datos
 * cambió entre el momento de lectura y escritura.
 * 
 * Implementa el requerimiento 9.5: Manejo seguro de actualizaciones concurrentes.
 */
public class ConcurrentRatingUpdateException extends RatingCalculationException {

    private final String playerProfileId;
    private final String roleType;
    private final String priorityLevel;
    private final Long expectedVersion;
    private final Long actualVersion;

    /**
     * Construye una nueva excepción de actualización concurrente con el mensaje especificado.
     * 
     * @param message el mensaje de detalle que describe la causa del error
     */
    public ConcurrentRatingUpdateException(String message) {
        super(message);
        this.playerProfileId = null;
        this.roleType = null;
        this.priorityLevel = null;
        this.expectedVersion = null;
        this.actualVersion = null;
    }

    /**
     * Construye una nueva excepción de actualización concurrente con información detallada.
     * 
     * @param message el mensaje de detalle que describe la causa del error
     * @param playerProfileId el ID del jugador cuya calificación se intentó actualizar
     * @param roleType el tipo de rol que se intentó actualizar
     * @param priorityLevel el nivel de prioridad que se intentó actualizar
     */
    public ConcurrentRatingUpdateException(String message, String playerProfileId, 
                                         String roleType, String priorityLevel) {
        super(message);
        this.playerProfileId = playerProfileId;
        this.roleType = roleType;
        this.priorityLevel = priorityLevel;
        this.expectedVersion = null;
        this.actualVersion = null;
    }

    /**
     * Construye una nueva excepción de actualización concurrente con información de versioning.
     * 
     * @param message el mensaje de detalle que describe la causa del error
     * @param playerProfileId el ID del jugador cuya calificación se intentó actualizar
     * @param roleType el tipo de rol que se intentó actualizar
     * @param priorityLevel el nivel de prioridad que se intentó actualizar
     * @param expectedVersion la versión esperada de la entidad
     * @param actualVersion la versión actual de la entidad
     */
    public ConcurrentRatingUpdateException(String message, String playerProfileId, 
                                         String roleType, String priorityLevel,
                                         Long expectedVersion, Long actualVersion) {
        super(message);
        this.playerProfileId = playerProfileId;
        this.roleType = roleType;
        this.priorityLevel = priorityLevel;
        this.expectedVersion = expectedVersion;
        this.actualVersion = actualVersion;
    }

    /**
     * Construye una nueva excepción de actualización concurrente con mensaje y causa.
     * 
     * @param message el mensaje de detalle que describe la causa del error
     * @param cause la causa raíz del error (típicamente OptimisticLockingFailureException)
     */
    public ConcurrentRatingUpdateException(String message, Throwable cause) {
        super(message, cause);
        this.playerProfileId = null;
        this.roleType = null;
        this.priorityLevel = null;
        this.expectedVersion = null;
        this.actualVersion = null;
    }

    /**
     * Obtiene el ID del jugador cuya calificación se intentó actualizar.
     * 
     * @return el ID del jugador, o null si no está disponible
     */
    public String getPlayerProfileId() {
        return playerProfileId;
    }

    /**
     * Obtiene el tipo de rol que se intentó actualizar.
     * 
     * @return el tipo de rol, o null si no está disponible
     */
    public String getRoleType() {
        return roleType;
    }

    /**
     * Obtiene el nivel de prioridad que se intentó actualizar.
     * 
     * @return el nivel de prioridad, o null si no está disponible
     */
    public String getPriorityLevel() {
        return priorityLevel;
    }

    /**
     * Obtiene la versión esperada de la entidad durante la actualización.
     * 
     * @return la versión esperada, o null si no está disponible
     */
    public Long getExpectedVersion() {
        return expectedVersion;
    }

    /**
     * Obtiene la versión actual de la entidad que causó el conflicto.
     * 
     * @return la versión actual, o null si no está disponible
     */
    public Long getActualVersion() {
        return actualVersion;
    }

    /**
     * Indica si esta excepción contiene información de versioning para optimistic locking.
     * 
     * @return true si hay información de versiones disponible
     */
    public boolean hasVersioningInfo() {
        return expectedVersion != null && actualVersion != null;
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
        
        if (roleType != null) {
            message.append(" [Rol: ").append(roleType).append("]");
        }
        
        if (priorityLevel != null) {
            message.append(" [Prioridad: ").append(priorityLevel).append("]");
        }
        
        if (hasVersioningInfo()) {
            message.append(" [Versión esperada: ").append(expectedVersion)
                   .append(", Versión actual: ").append(actualVersion).append("]");
        }
        
        return message.toString();
    }
}