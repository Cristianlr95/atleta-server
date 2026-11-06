package com.atleta.demo.exception;

public class MatchTeamRequiredException extends RuntimeException {
    public MatchTeamRequiredException() {
        super("El partido no tiene equipo asociado. Pide al organizador que lo configure.");
    }
}
