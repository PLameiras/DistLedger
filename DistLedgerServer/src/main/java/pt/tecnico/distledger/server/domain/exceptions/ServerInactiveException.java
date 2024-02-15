package pt.tecnico.distledger.server.domain.exceptions;

public class ServerInactiveException extends Exception {
    public ServerInactiveException() {
        super("Server inactive.");
    }
    public ServerInactiveException(boolean server) {
        super("Secondary server inactive. Writing operations unavailable.");
    }
}