package pt.tecnico.distledger.server.domain.exceptions;

public class BalanceNotZeroException extends Exception{
    public BalanceNotZeroException(String userId) {
        super("Account \"" + userId + "\" balance is not zero");
    }
}
