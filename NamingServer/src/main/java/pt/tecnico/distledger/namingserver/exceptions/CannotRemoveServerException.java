package pt.tecnico.distledger.namingserver.exceptions;

public class CannotRemoveServerException extends Exception {
    public CannotRemoveServerException(String service, String address) {
        super("Cannot remove server at " + address + " from service " + service);
    }
}
