package org.ftf.koifishveterinaryservicecenter.exception;

public class PrescriptionNotFoundException extends RuntimeException {
    public PrescriptionNotFoundException(String message) {
        super(message);
    }

    public PrescriptionNotFoundException() {
    }
}
