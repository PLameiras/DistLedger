package pt.tecnico.distledger.userclient;

import pt.tecnico.distledger.userclient.grpc.UserService;

import java.util.Scanner;

public class CommandParser {

    private static final String SPACE = " ";
    private static final String CREATE_ACCOUNT = "createAccount";
    private static final String TRANSFER_TO = "transferTo";
    private static final String BALANCE = "balance";
    private static final String HELP = "help";
    private static final String EXIT = "exit";

    private final UserService userService;

    public CommandParser(UserService userService) {
        this.userService = userService;
    }

    void parseInput() {

        Scanner scanner = new Scanner(System.in);
        boolean exit = false;

        while (!exit) {
            System.out.print("> ");
            String line = scanner.nextLine().trim();
            String cmd = line.split(SPACE)[0];

            try {
                switch (cmd) {
                    case CREATE_ACCOUNT:
                        this.createAccount(line);
                        break;

                    case TRANSFER_TO:
                        this.transferTo(line);
                        break;

                    case BALANCE:
                        this.balance(line);
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
            catch (Exception e){
                System.err.println(e.getMessage());
            }
        }
    }

    private void createAccount(String line){
        String[] split = line.split(SPACE);

        if (split.length != 3){
            if(userService.isDebug()) System.err.println("CommandParser: Bad arguments!");
            this.printUsage();
            return;
        }

        String server = split[1];
        String username = split[2];

        if(userService.isDebug()) System.err.println("CommandParser: Proceeding to 'createAccount' on server '" + server + "' of '" + username + "'");
        this.userService.createAccount(server, username);

    }

    private void balance(String line){
        String[] split = line.split(SPACE);

        if (split.length != 3){
            if(userService.isDebug()) System.err.println("CommandParser: Bad arguments!");
            this.printUsage();
            return;
        }
        String server = split[1];
        String username = split[2];

        if(userService.isDebug()) System.err.println("CommandParser: Proceeding to show 'balance' on server '" + server + "' of '" + username + "'");
        this.userService.balance(server, username);

    }

    private void transferTo(String line){
        String[] split = line.split(SPACE);

        if (split.length != 5){
            if(userService.isDebug()) System.err.println("CommandParser: Bad arguments!");
            this.printUsage();
            return;
        }
        String server = split[1];
        String from = split[2];
        String dest = split[3];
        Integer amount = Integer.valueOf(split[4]);

        if(userService.isDebug()) System.err.println("CommandParser: Proceeding to transfer on server '" + server + "' the amount " + amount + " from '" +  from + "' to '" + dest + "'");
        this.userService.transferTo(server, from, dest, amount);

    }

    private void printUsage() {
        System.out.println("Usage:\n" +
                "- createAccount <server> <username>\n" +
                "- balance <server> <username>\n" +
                "- transferTo <server> <username_from> <username_to> <amount>\n" +
                "- exit\n");
    }
}
