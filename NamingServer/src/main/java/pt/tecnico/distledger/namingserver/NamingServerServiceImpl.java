package pt.tecnico.distledger.namingserver;

import io.grpc.stub.StreamObserver;
import pt.tecnico.distledger.namingserver.domain.NamingServerState;
import pt.tecnico.distledger.namingserver.domain.ServerEntry;
import pt.ulisboa.tecnico.distledger.contract.NamingServer.*;
import pt.ulisboa.tecnico.distledger.contract.NamingServerServiceGrpc;
import pt.tecnico.distledger.namingserver.exceptions.*;
import static io.grpc.Status.UNAVAILABLE;


import java.util.*;

public class NamingServerServiceImpl extends NamingServerServiceGrpc.NamingServerServiceImplBase {

    private NamingServerState state;

    public NamingServerServiceImpl(NamingServerState state) {
        this.state = state;
    }

    @Override
    public void register(RegisterRequest request, StreamObserver<RegisterResponse> responseObserver) {
        try {
            if (state.isDebug()) System.err.println("\tServerService: Proceeding to 'register' new '" + request.getService() + "' server at '" + request.getAddress() + "' qualified as '" + request.getQualifier() + "'");
            state.register(request.getAddress(), request.getQualifier(), request.getService());
            RegisterResponse response = RegisterResponse.newBuilder().build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (CannotRegisterServerException e) {
            responseObserver.onError(UNAVAILABLE.withDescription(e.getMessage()).asRuntimeException());
            return;
        }
        if (state.isDebug()) System.err.println("\tServerService: Registration successful");
    }

    @Override
    public void lookup(LookupRequest request, StreamObserver<LookupResponse> responseObserver) {
        if (state.isDebug()) System.err.println("\tServerService: Proceeding to 'lookup' '" + request.getService() + "' servers qualified as '" + request.getQualifier() + "'");
        List<ServerEntry> servers = state.lookup(request.getService(), request.getQualifier());
        LookupResponse.Builder response = LookupResponse.newBuilder();
        for (ServerEntry server : servers) {
            if (state.isDebug())
                System.err.println("\t\t-> Found '" + request.getService() + "' server qualified as '" + request.getQualifier() + "' at '" + server.getAddress() + "'");
            response.addServers(ServerEntryResponse.newBuilder().setAddress(server.getAddress()).setQualifier(server.getQualifier()).build());
        }
        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
        if (state.isDebug()) System.err.println("\tServerService: Lookup successful");
    }

    @Override
    public void delete(DeleteRequest request, StreamObserver<DeleteResponse> responseObserver) {
        try {
        if(state.isDebug()) System.err.println("\tServerService: Proceeding to 'delete' '" + request.getService() + "' server at '" + request.getAddress() + "'");
        state.delete(request.getAddress(), request.getService());
        responseObserver.onNext(DeleteResponse.getDefaultInstance());
        responseObserver.onCompleted();
        } catch (CannotRemoveServerException e) {
            responseObserver.onError(UNAVAILABLE.withDescription(e.getMessage()).asRuntimeException());
            return;
        }
        if (state.isDebug()) System.err.println("\tServerService: Deletion successful");
    }
}