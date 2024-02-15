package pt.tecnico.distledger.server.domain.exceptions;

public class NotEnoughBalanceException extends Exception {

    public NotEnoughBalanceException(String userId) {
        super("Account \"" + userId + "\" balance is not enough");
    }
}