package pt.tecnico.distledger.adminclient;

import pt.tecnico.distledger.adminclient.grpc.AdminService;

public class AdminClientMain {

    public static void main(String[] args) {

        final boolean debug = (System.getProperty("debug") != null);

        System.out.println(AdminClientMain.class.getSimpleName());
        if (debug) System.out.println("Debug enabled");

        final String host = "localhost";
        final int port = 5001;

        // start Admin
        CommandParser parser = new CommandParser(new AdminService(host, port, debug));
        parser.parseInput();

    }

}
