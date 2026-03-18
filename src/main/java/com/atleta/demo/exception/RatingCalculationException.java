package com.atleta.demo.exception;

/**
 * Excepción base para errores relacionados con el cálculo de calificaciones de jugadores.
 * Esta excepción se lanza cuando ocurren errores durante el proceso de cálculo de calificaciones,
 * incluyendo problemas de validación de datos, errores de cálculo matemático, o fallos en la
 * aplicación de reglas de negocio.
 * 
 * Implementa el requerimiento 9.4: Manejo de errores y excepciones en el sistema de calificación.
 */
public class RatingCalculationException extends RuntimeException {

    /**
     * Construye una nueva excepción de cálculo de calificación con el mensaje especificado.
     * 
     * @param message el mensaje de detalle que describe la causa del error
     */
    public RatingCalculationException(String message) {
        super(message);
    }

    /**
     * Construye una nueva excepción de cálculo de calificación con el mensaje especificado
     * y la causa raíz del error.
     * 
     * @param message el mensaje de detalle que describe la causa del error
     * @param cause la causa raíz del error (puede ser null)
     */
    public RatingCalculationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Construye una nueva excepción de cálculo de calificación con la causa especificada.
     * El mensaje de detalle se establece automáticamente basado en la causa.
     * 
     * @param cause la causa raíz del error (no debe ser null)
     */
    public RatingCalculationException(Throwable cause) {
        super(cause);
    }
}