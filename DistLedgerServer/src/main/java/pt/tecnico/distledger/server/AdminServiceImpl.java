package pt.tecnico.distledger.server;

import io.grpc.stub.StreamObserver;
import pt.tecnico.distledger.server.domain.*;
import pt.tecnico.distledger.server.domain.exceptions.*;
import pt.tecnico.distledger.server.domain.operation.Operation;
import pt.tecnico.distledger.server.grpc.Converter;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.admin.AdminServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions.LedgerState;

import java.util.List;

import static io.grpc.Status.UNAVAILABLE;

public class AdminServiceImpl extends AdminServiceGrpc.AdminServiceImplBase {

private ServerState state;

    public AdminServiceImpl(ServerState state1) {
        this.state = state1;
    }

    @Override
    public void activate(ActivateRequest request, StreamObserver<ActivateResponse> responseObserver) {
        try {
            if (state.isDebug()) System.err.println("\tAdminService: Proceeding to 'activate' on " + state.getServerQualifier());
            state.activate();
            ActivateResponse response = ActivateResponse.newBuilder().build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (ServerAlreadyActiveException e) {
            responseObserver.onError(UNAVAILABLE.withDescription(e.getMessage()).asRuntimeException());
        }
        if(state.isDebug()) System.err.println("\tAdminService: Activation successful");
    }

    @Override
    public void deactivate(DeactivateRequest request, StreamObserver<DeactivateResponse> responseObserver) {
        try {
            if (state.isDebug()) System.err.println("\tAdminService: Proceeding to 'deactivate' on " + state.getServerQualifier());
            state.deactivate();
            DeactivateResponse response = DeactivateResponse.newBuilder().build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }  catch (ServerAlreadyInactiveException e) {
            responseObserver.onError(UNAVAILABLE.withDescription(e.getMessage()).asRuntimeException());
        }
        if(state.isDebug()) System.err.println("\tAdminService: Deactivation successful");
    }

    @Override
    public void getLedgerState(getLedgerStateRequest request, StreamObserver<getLedgerStateResponse> responseObserver) {
        try {
            if(state.isDebug()) System.err.println("\tAdminService: Proceeding to 'getLedgerState' on " + state.getServerQualifier());
            List<Operation> operations = state.getLedgerState();
            Converter converter = new Converter(operations);
            LedgerState ledgerState = LedgerState.newBuilder().addAllLedger(converter.getLedgerOperations()).build();
            getLedgerStateResponse response = getLedgerStateResponse.newBuilder().setLedgerState(ledgerState).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (ServerInactiveException e) {
            responseObserver.onError(UNAVAILABLE.withDescription(e.getMessage()).asRuntimeException());
        }
        if(state.isDebug()) System.err.println("\tAdminService: Ledger state procurement successful");
    }

    @Override
    public void gossip(GossipRequest request, StreamObserver<GossipResponse> responseObserver) {
        try {
            if(state.isDebug()) System.err.println("\tAdminService: Proceeding to 'gossip' on " + state.getServerQualifier());
            state.getServerService().propagateState(state.gossip(), state.getTimeStampValues());
            GossipResponse response = GossipResponse.newBuilder().build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (ServerInactiveException | NoSecondaryServerException e) {
            responseObserver.onError(UNAVAILABLE.withDescription(e.getMessage()).asRuntimeException());
        }
    }
}