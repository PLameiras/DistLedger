package pt.tecnico.distledger.userclient;


import pt.tecnico.distledger.userclient.grpc.UserService;

public class UserClientMain {
    public static void main(String[] args) {

        final boolean debug = (System.getProperty("debug") != null);

        System.out.println(UserClientMain.class.getSimpleName());
        if (debug) System.out.println("Debug enabled");

        final String host = "localhost";
        final int port = 5001;

        // start User
        CommandParser parser = new CommandParser(new UserService(host, port, debug));
        parser.parseInput();

    }
}