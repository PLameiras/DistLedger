package pt.tecnico.distledger.adminclient.grpc;

import io.grpc.StatusRuntimeException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.ulisboa.tecnico.distledger.contract.NamingServerServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.NamingServerServiceGrpc.NamingServerServiceBlockingStub;
import pt.ulisboa.tecnico.distledger.contract.NamingServer.LookupRequest;
import pt.ulisboa.tecnico.distledger.contract.NamingServer.LookupResponse;
import pt.ulisboa.tecnico.distledger.contract.NamingServer.ServerEntryResponse;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminServiceGrpc.AdminServiceBlockingStub;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.*;

public class AdminService {

    private boolean debug;
    private Map<String, AdminServiceBlockingStub> stubs;
    private NamingServerServiceBlockingStub namingServerStub;

    public AdminService(String host, Integer port) {
        this.debug = false;
        final String target = host + ":" + port;
        this.stubs = new HashMap<>();
        final ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        namingServerStub = NamingServerServiceGrpc.newBlockingStub(channel);
    }

    public AdminService(String host, Integer port, boolean debug) {
        this(host, port);
        this.debug = debug;
    }

    public boolean isDebug() {
        return this.debug;
    }

    private boolean checkServerIsKnown(String qualifier) {
        return stubs.containsKey(qualifier);
    }

    private void addStub(String qualifier, AdminServiceBlockingStub stub) {
        stubs.put(qualifier, stub);
    }

    private AdminServiceBlockingStub getStub(String qualifier) {
        return stubs.get(qualifier);
    }

    private NamingServerServiceBlockingStub getNamingServerStub() {
        return namingServerStub;
    }

    private Optional<AdminServiceBlockingStub> buildChannelAndStub(String qualifier) {
        Optional<AdminServiceBlockingStub> stub = Optional.empty();
        AdminServiceBlockingStub serverStub;
        // Get servers with the given qualifier and create and store stub
        for (ServerEntryResponse server : this.lookup("DistLedger", qualifier).getServersList()) {
            if(this.isDebug()) System.err.println("\tAdminService: Building channel and stub for '" + server.getQualifier() + "'");
            if (!checkServerIsKnown(server.getQualifier())) {
                addStub(server.getQualifier(), (serverStub = AdminServiceGrpc.newBlockingStub(ManagedChannelBuilder.forTarget(server.getAddress()).usePlaintext().build())));
                if (server.getQualifier().equals(qualifier)) {
                    if (this.isDebug()) System.err.println("\tAdminService: Target server found at '" + server.getAddress() + "'");
                    stub = Optional.ofNullable(serverStub);
                }
            }
        }
        return stub;
    }

    private Optional<AdminServiceBlockingStub> getServerStub(String qualifier) {
        if (checkServerIsKnown(qualifier)) return Optional.ofNullable(getStub(qualifier));
        else return buildChannelAndStub(qualifier);
    }

    public void activate(String qualifier) {
        ActivateResponse response;
        Optional<AdminServiceBlockingStub> stub = getServerStub(qualifier);
        if(stub.isEmpty()) return;
        if(this.isDebug()) System.err.println("\tAdminService: requesting activate");
        try {
            response = stub.get().activate(ActivateRequest.getDefaultInstance());
        } catch (StatusRuntimeException e) {
            System.out.println(e.getStatus().getCode());
            if (e.getStatus().getDescription() != null) System.out.println(e.getStatus().getDescription());
            return;
        }
        System.out.print("OK\n" + response + "\n");
    }

    public void deactivate(String qualifier) {
        DeactivateResponse response;
        Optional<AdminServiceBlockingStub> stub = getServerStub(qualifier);
        if(stub.isEmpty()) return;
        if (this.isDebug()) System.err.println("\tAdminService: requesting deactivate");
        try {
            response = stub.get().deactivate(DeactivateRequest.getDefaultInstance());
        } catch (StatusRuntimeException e) {
            System.out.println(e.getStatus().getCode());
            if (e.getStatus().getDescription() != null) System.out.println(e.getStatus().getDescription());
            return;
        }
        System.out.print("OK\n" + response + "\n");
    }

    public void getLedgerState(String qualifier) {
        getLedgerStateResponse response;
        Optional<AdminServiceBlockingStub> stub = getServerStub(qualifier);
        if(stub.isEmpty()) return;
        if(this.isDebug()) System.err.println("\tAdminService: requesting ledger state");
        try {
            response = stub.get().getLedgerState(getLedgerStateRequest.getDefaultInstance());
        } catch (StatusRuntimeException e) {
            System.out.println(e.getStatus().getCode());
            if (e.getStatus().getDescription() != null) System.out.println(e.getStatus().getDescription());
            return;
        }
        System.out.print("OK\n" + response + "\n");
    }

    public void gossip(String qualifier) {
        GossipResponse response;
        Optional<AdminServiceBlockingStub> stub = getServerStub(qualifier);
        if(stub.isEmpty()) return;
        try {
            response = stub.get().gossip(GossipRequest.getDefaultInstance());
        } catch (StatusRuntimeException e) {
            System.out.println(e.getStatus().getCode());
            if (e.getStatus().getDescription() != null) System.out.println(e.getStatus().getDescription());
            return;
        }
        System.out.print("OK\n" + response + "\n");
    }

    public LookupResponse lookup(String service, String qualifier) {
        if(isDebug()) System.err.println("\tAdminService: Proceeding to 'lookup' servers as " + qualifier + " on " + service);
        LookupResponse response = getNamingServerStub().lookup(LookupRequest.newBuilder().setService(service).setQualifier(qualifier).build());
        if(this.isDebug()) System.err.println("\tAdminService: Received lookup response with " + response.getServersList().size() + " elements");
        return response;
    }
}