package com.atleta.demo.exception;

public class CaptainTransferRequiredException extends RuntimeException {
    public CaptainTransferRequiredException() {
        super("Transfiere la capitanía de tus equipos con otros integrantes antes de eliminar tu cuenta.");
    }
}
