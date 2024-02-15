package pt.tecnico.distledger.server.domain.exceptions;

public class NoSecondaryServerException extends Exception {
    public NoSecondaryServerException() {
        super("No secondary server active.");
    }
}
