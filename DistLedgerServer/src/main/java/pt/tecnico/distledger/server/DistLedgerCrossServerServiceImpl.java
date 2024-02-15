package pt.tecnico.distledger.server;

import io.grpc.stub.StreamObserver;
import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.exceptions.*;
import pt.tecnico.distledger.server.grpc.Reverter;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.CrossServerDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.distledgerserver.DistLedgerCrossServerServiceGrpc;

import static io.grpc.Status.UNAVAILABLE;

public class DistLedgerCrossServerServiceImpl extends DistLedgerCrossServerServiceGrpc.DistLedgerCrossServerServiceImplBase {

    private ServerState state;

    public DistLedgerCrossServerServiceImpl(ServerState state) {
        this.state = state;
    }

    @Override
    public void propagateState(PropagateStateRequest request, StreamObserver<PropagateStateResponse> responseObserver) {
        if(state.isDebug()) System.err.println("\tCrossServerService: Proceeding to 'propagateState' to server '" + state.getServerQualifier() + "' of " + request.getState().getLedgerList().size() + " operations");
        try {
            Reverter reverter = new Reverter(request.getState().getLedgerList());
            state.receiveState(reverter.getLedgerOperations(), request.getReplicaTSList());
            PropagateStateResponse response = PropagateStateResponse.getDefaultInstance();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (ServerInactiveException | InterruptedException e) {
            responseObserver.onError(UNAVAILABLE.withDescription(e.getMessage()).asRuntimeException());
        }
        if(state.isDebug()) System.err.println("\tCrossServerService: State propagation successful");
    }
}