package pt.tecnico.distledger.server.grpc;

import java.util.*;
import java.util.stream.Collectors;

import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.exceptions.NoSecondaryServerException;
import pt.tecnico.distledger.server.domain.exceptions.ServerInactiveException;
import pt.tecnico.distledger.server.domain.operation.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.LedgerState;
import pt.ulisboa.tecnico.distledger.contract.NamingServer;
import pt.ulisboa.tecnico.distledger.contract.NamingServerServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.NamingServerServiceGrpc.NamingServerServiceBlockingStub;
import pt.ulisboa.tecnico.distledger.contract.NamingServer.DeleteRequest;
import pt.ulisboa.tecnico.distledger.contract.NamingServer.LookupRequest;
import pt.ulisboa.tecnico.distledger.contract.NamingServer.RegisterRequest;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.PropagateStateRequest;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceBlockingStub;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.DistLedgerCrossServerServiceGrpc;

public class ServerService {
    private NamingServerServiceBlockingStub namingServerStub;
    private TreeMap<String, DistLedgerCrossServerServiceBlockingStub> crossServerStub;

    private ServerState state;

    private String address;
    private String port;
    private String service;
    private boolean debug;
    private String qualifier;

    public ServerService(String address, String port, String service,  String qualifier, boolean debug) {
        this.debug = debug;
        this.address = address;
        this.port = port;
        this.service = service;
        this.buildChannelAndStubNamingServer();
        this.qualifier = qualifier;
        this.crossServerStub = new TreeMap<>();
    }

    public String getServerQualifier() {
        return this.qualifier;
    }
    public String getServerService() {
        return this.service;
    }
    public String getServerAddress() {
        return this.address;
    }
    public String getServerPort() {
        return this.port;
    }
    public void setState(ServerState state) {
        this.state = state;
    }
    public String getSelf() {
        return getServerAddress() + ":" + getServerPort();
    }

    public boolean isDebug() {
        return this.debug;
    }

    private void buildChannelAndStubNamingServer() {
        String target = "localhost" + ":" + "5001";
        if(this.isDebug()) System.err.println("\t\t\tServerService: Building channel and stub to naming server at '" + target + "'");
        final ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        namingServerStub = NamingServerServiceGrpc.newBlockingStub(channel);
    }

    private Optional<DistLedgerCrossServerServiceBlockingStub> buildChannelAndStubCrossServer(String address, String qualifier) {
        if (this.isDebug())
            System.err.println("\t\t\tServerService: Building channel and stub to cross server at '" + address + "'");
        final ManagedChannel channel = ManagedChannelBuilder.forTarget(address).usePlaintext().build();
        Optional.ofNullable((crossServerStub.put(qualifier, DistLedgerCrossServerServiceGrpc.newBlockingStub(channel))));
        if(this.isDebug()) System.err.println("\t\t\tServerService: Build successful");
        return Optional.of(crossServerStub.get(qualifier));
    }

    private void addCrossServerStub() {
        List<NamingServer.ServerEntryResponse> servers = lookup("DistLedger", "");
        for(NamingServer.ServerEntryResponse server : servers) {
             if(!this.hasStub(server.getQualifier())) if(buildChannelAndStubCrossServer(server.getAddress(), server.getQualifier()).isPresent()) this.state.addTimeStamp(qualifier);
        }
    }

    private Optional<DistLedgerCrossServerServiceBlockingStub> getCrossServerStub(String qualifier) {
        return Optional.ofNullable(crossServerStub.get(qualifier));
    }

    private List<DistLedgerCrossServerServiceBlockingStub> getCrossServerStubs() {
        return Collections.unmodifiableList(new ArrayList<>(crossServerStub.values()));
    }

    private boolean hasStub(String qualifier) {
        return this.crossServerStub.containsKey(qualifier);
    }

    private NamingServerServiceBlockingStub getNamingServerStub() {
        return namingServerStub;
    }


    public void initialize() {
        for (NamingServer.ServerEntryResponse server : lookup(getServerService(), "")) {
            System.err.println("\t\t\tServerService: Building channel and stub to cross server at '" + server.getAddress() + "'");
            final ManagedChannel channel = ManagedChannelBuilder.forTarget(server.getAddress()).usePlaintext().build();
            crossServerStub.put(server.getQualifier(), DistLedgerCrossServerServiceGrpc.newBlockingStub(channel));
            this.state.addTimeStamp(server.getQualifier());
        }
    }

    public synchronized void register() {
        if(this.isDebug()) System.err.println("\t\t\tServerService: Proceeding to 'register' server on '" + getServerService() + "' as '" + getServerQualifier() + "' at '" + getSelf() + "'");
        try {
            this.getNamingServerStub().register(RegisterRequest.newBuilder().setAddress(getSelf()).setQualifier(this.getServerQualifier()).setService(this.getServerService()).build());
        } catch (RuntimeException e) {
            e.printStackTrace();
            return;
        }
        if(this.isDebug()) System.err.println("\t\t\tServerService: Registration successful");
    }

    public synchronized List<NamingServer.ServerEntryResponse> lookup(String service, String qualifier) {
        if(isDebug()) System.err.println("\t\t\tServerService: Proceeding to 'lookup' servers as '" + qualifier + "' on '" + service + "' that are not qualified as '" + this.getServerQualifier() + "'");
        List<NamingServer.ServerEntryResponse> response = this.getNamingServerStub().lookup(LookupRequest.newBuilder().setService(service).setQualifier(qualifier).build()).getServersList().stream().filter(a -> !a.getAddress().equals(this.getSelf())).collect(Collectors.toList());
        if(this.isDebug()) System.err.println("\t\t\tServerService: Lookup successful. Response with size " + response.size() + " received");
        return response;
    }

    public synchronized void propagateState(List<Operation> operations, List<Integer> replicaTS) throws ServerInactiveException, NoSecondaryServerException {
        if(this.getCrossServerStubs().size() != 2) addCrossServerStub();

        if (this.isDebug()) System.err.println("\t\t\tServerService: Proceeding to 'propagateState'");
        Converter converter = new Converter(operations);
        LedgerState ledger = LedgerState.newBuilder().addAllLedger(converter.getLedgerOperations()).build();

        if (this.isDebug()) System.err.println("\t\t\tServerService: Propagate to secondary server");
        try {
            for (DistLedgerCrossServerServiceBlockingStub stub : this.getCrossServerStubs())
                stub.propagateState(PropagateStateRequest.newBuilder().setState(ledger).addAllReplicaTS(replicaTS).build());
        } catch (RuntimeException e) {
            throw new ServerInactiveException(true);
        }

        if(this.isDebug()) System.err.println("\t\t\tServerService: State propagation successful");
    }

    public synchronized void delete() {
        if(this.isDebug()) System.err.println("\t\t\tServerService: Proceeding to 'delete' server at '" + getSelf() + "' on '" + getServerService() + "'");
        try {
            this.getNamingServerStub().delete(DeleteRequest.newBuilder().setAddress(getSelf()).setService(this.getServerService()).build());
        } catch (RuntimeException e) {
            e.printStackTrace();
            return;
        }
        if(this.isDebug()) System.err.println("\t\t\tServerService: Deletion successful");
    }
}
