package pt.tecnico.distledger.server.domain.exceptions;

public class NoSuchAccountException extends Exception {
    public NoSuchAccountException(String userId) {
        super("Account \"" + userId + "\" does not exist");
    }
}
