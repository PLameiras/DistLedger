package pt.tecnico.distledger.namingserver;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import pt.tecnico.distledger.namingserver.domain.*;

import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class NamingServerMain {

    public static void main(String[] args) {

        final boolean debug = (System.getProperty("debug") != null);
        if (debug) System.out.println("Debug enabled");
        final int port = 5001;

        NamingServerState state = new NamingServerState(debug);
        final BindableService namingServerImpl = new NamingServerServiceImpl(state);

        // Create a new server to listen on port
        Server server = ServerBuilder.forPort(port).addService(namingServerImpl).build();

        // Start the server
        try {
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Server threads are running in the background.
        System.out.println("Naming server started");

        Scanner scan = new Scanner(System.in);
        System.out.println("Press ENTER to shut down the server.");

        // Server shutdown when JVM terminated
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutting down.");
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
        System.out.println("Shutting down.");
        server.shutdown();

        // Do not exit the main thread. Wait until server is terminated.
        try {
            if (server.awaitTermination(5, TimeUnit.SECONDS)) {
                return;
            }
            server.shutdownNow();
        } catch (InterruptedException e) {
            throw new RuntimeException("Unable to shutdown server");
        }
    }
}
