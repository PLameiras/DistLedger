package pt.tecnico.distledger.server;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import pt.tecnico.distledger.server.domain.*;
import pt.tecnico.distledger.server.grpc.ServerService;

import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class ServerMain {

    public static void main(String[] args)  {

        final boolean debug = (System.getProperty("debug") != null);

        System.out.println(ServerMain.class.getSimpleName());

        // receive and print arguments
        System.out.printf("Received %d arguments%n", args.length);
        for (int i = 0; i < args.length; i++) {
            System.out.printf("arg[%d] = %s%n", i, args[i]);
        }
        if (debug) System.out.println("Debug enabled");

        // check arguments
        if (args.length != 2) {
            if (args.length < 2) System.err.println("Argument(s) missing!");
            else System.err.println("Too many arguments!");
            System.err.println("Usage: \n\tmvn exec:java -Dexec.args=\"<port> <qualifier>\"\n\tTo enable debugging run maven with -Ddebug option");
            return;
        }

        final int port = Integer.parseInt(args[0]);


        ServerState state = new ServerState(new ServerService("localhost", args[0], "DistLedger", args[1], debug));
        final BindableService userImpl = new UserServiceImpl(state);
        final BindableService adminImpl = new AdminServiceImpl(state);
        final BindableService crossImpl = new DistLedgerCrossServerServiceImpl(state);

        // Create a new server to listen on port
        Server server = ServerBuilder.forPort(port).addService(userImpl).addService(adminImpl).addService(crossImpl).build();

        // Start the server
        try {
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Server threads are running in the background.
        System.out.println("Server started");

        Scanner scan = new Scanner(System.in);
        System.out.println("Press ENTER to delete the server.");

        // Server shutdown when JVM terminated
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nDeleting server '" + state.getServerAddress() + "' from '" + state.getService() + "' service.");
            state.delete();
            System.out.println("Shutting down.");
            server.shutdown();
            try {
                if (server.awaitTermination(5, TimeUnit.SECONDS)) {
                    return;
                }
                server.shutdownNow();
            } catch (InterruptedException e) {
                throw new RuntimeException("Unable to shutdown server");
            }
        }));

        scan.nextLine();
    }
}
