package pt.tecnico.distledger.namingserver.exceptions;

public class NoSuchServiceException extends Exception {
    public NoSuchServiceException(String serviceName) {
        super("No service entry found for service " + serviceName);
    }
}
