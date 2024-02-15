package pt.tecnico.distledger.adminclient;

import pt.tecnico.distledger.adminclient.grpc.AdminService;

import java.util.Scanner;

public class CommandParser {

    private static final String SPACE = " ";
    private static final String ACTIVATE = "activate";
    private static final String DEACTIVATE = "deactivate";
    private static final String GET_LEDGER_STATE = "getLedgerState";
    private static final String GOSSIP = "gossip";
    private static final String HELP = "help";
    private static final String EXIT = "exit";

    private final AdminService adminService;
    public CommandParser(AdminService adminService) {
        this.adminService = adminService;
    }
    void parseInput() {

        Scanner scanner = new Scanner(System.in);
        boolean exit = false;

        while (!exit) {
            System.out.print("> ");
            String line = scanner.nextLine().trim();

            String[] split = line.split(SPACE);

            if (split.length != 2){
                if(adminService.isDebug()) System.err.println("CommandParser: Bad arguments!");
                this.printUsage();
                continue;
            }

            String qualifier = split[1];

            switch (split[0]) {
                case ACTIVATE:
                    this.activate(qualifier);
                    break;

                case DEACTIVATE:
                    this.deactivate(qualifier);
                    break;

                case GET_LEDGER_STATE:
                    this.dump(qualifier);
                    break;

                case GOSSIP:
                    this.gossip(qualifier);
                    break;

                case HELP:
                    this.printUsage();
                    break;

                case EXIT:
                    exit = true;
                    break;

                default:
                    System.out.println("Bad command!\n");
                    this.printUsage();
                    break;
            }

        }
    }

    private void activate(String server){
        if(adminService.isDebug()) System.err.println("CommandParser: Proceeding to 'activate' on " + server + "'");
        adminService.activate(server);
    }

    private void deactivate(String server){
        if(adminService.isDebug()) System.err.println("CommandParser: Proceeding to 'deactivate' on " + server + "'");
        adminService.deactivate(server);
    }

    private void dump(String server){
        if(adminService.isDebug()) System.err.println("CommandParser: Proceeding to 'getLedgerState' on " + server + "'");
        adminService.getLedgerState(server);
    }

    @SuppressWarnings("unused")
    private void gossip(String server){
        if(adminService.isDebug()) System.err.println("CommandParser: Proceeding to 'gossip' on '" + server + "'");
        adminService.gossip(server);
    }

    private void printUsage() {
        System.out.println("Usage:\n" +
                "- activate <server>\n" +
                "- deactivate <server>\n" +
                "- getLedgerState <server>\n" +
                "- gossip <server>\n" +
                "- exit\n");
    }

}
