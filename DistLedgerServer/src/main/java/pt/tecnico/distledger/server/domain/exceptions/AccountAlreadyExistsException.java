package pt.tecnico.distledger.server.domain.exceptions;

public class AccountAlreadyExistsException extends Exception {
    public AccountAlreadyExistsException(String userId) {
        super("Account \"" + userId + "\" already exists");
    }
}
