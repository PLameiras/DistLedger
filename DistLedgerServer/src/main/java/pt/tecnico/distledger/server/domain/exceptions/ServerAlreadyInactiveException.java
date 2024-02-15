package pt.tecnico.distledger.server.domain.exceptions;

public class ServerAlreadyInactiveException extends Exception {
    public ServerAlreadyInactiveException() {
        super("Server already inactive.");
    }
}