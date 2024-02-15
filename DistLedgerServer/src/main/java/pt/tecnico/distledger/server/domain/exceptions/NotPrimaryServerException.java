package pt.tecnico.distledger.server.domain.exceptions;

public class NotPrimaryServerException extends Exception {
    public NotPrimaryServerException() {
        super("The server provided is not the primary.");
    }
}