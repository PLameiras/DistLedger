package pt.tecnico.distledger.server.domain.exceptions;

public class ServerAlreadyActiveException extends Exception {
    public ServerAlreadyActiveException() {
        super("Server already active.");
    }
}