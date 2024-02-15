package pt.tecnico.distledger.server;

import io.grpc.stub.StreamObserver;
import pt.tecnico.distledger.server.domain.*;
import pt.tecnico.distledger.server.domain.operation.CreateOp;
import pt.tecnico.distledger.server.domain.operation.Operation;
import pt.tecnico.distledger.server.domain.operation.TransferOp;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.*;
import pt.ulisboa.tecnico.distledger.contract.user.UserServiceGrpc;
import pt.tecnico.distledger.server.domain.exceptions.*;

import static io.grpc.Status.*;

public class UserServiceImpl extends UserServiceGrpc.UserServiceImplBase {

    private ServerState state;

    public UserServiceImpl(ServerState state1) {
        this.state = state1;
    }

    @Override
    public void createAccount(CreateAccountRequest request, StreamObserver<CreateAccountResponse> responseObserver) {
        try {
            if(state.isDebug()) System.err.println("\tUserService: Proceeding to 'createAccount' " + state.getServerQualifier() + " of " + request.getUserId());
            CreateOp op = new CreateOp(request.getUserId(), request.getPrevTSList());
            state.executeUpdate(op);
            CreateAccountResponse response = CreateAccountResponse.newBuilder().addAllTS(op.getTimeStamps()).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            if(state.isDebug()) System.err.println("\tUserService: Account creation successful");
        } catch (ServerInactiveException | InterruptedException e) {
            responseObserver.onError(UNAVAILABLE.withDescription(e.getMessage()).asRuntimeException());
        } catch (AccountAlreadyExistsException | NotEnoughBalanceException | NoSuchAccountException e) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void balance(BalanceRequest request, StreamObserver<BalanceResponse> responseObserver) {
        try {
            if(state.isDebug()) System.err.println("\tUserService: Proceeding to show 'balance' on " + state.getServerQualifier() + " of " + request.getUserId());
            int value = state.executeQuery(new Operation(request.getUserId(), request.getPrevTSList()));
            BalanceResponse response = BalanceResponse.newBuilder().setValue(value).addAllValueTS(state.getTimeStampValues()).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            if(state.isDebug()) System.err.println("\tUserService: Balance procurement successful");
        } catch (ServerInactiveException | InterruptedException e) {
            responseObserver.onError(UNAVAILABLE.withDescription(e.getMessage()).asRuntimeException());
        } catch (NoSuchAccountException e) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void transferTo(TransferToRequest request, StreamObserver<TransferToResponse> responseObserver) {
        try {
            if(state.isDebug()) System.err.println("\tUserService: Proceeding to transfer on " + state.getServerQualifier() + " from " + request.getAccountFrom() + " to " + request.getAccountTo() + " the amount " + request.getAmount());
            TransferOp op = new TransferOp(request.getAccountFrom(), request.getAccountTo(), request.getAmount(), request.getPrevTSList());
            state.executeUpdate(op);
            TransferToResponse response = TransferToResponse.newBuilder().addAllTS(op.getTimeStamps()).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            if(state.isDebug()) System.err.println("\tUserService: Transfer successful");
        } catch (ServerInactiveException | InterruptedException e) {
            responseObserver.onError(UNAVAILABLE.withDescription(e.getMessage()).asRuntimeException());
        } catch (NoSuchAccountException | NotEnoughBalanceException | AccountAlreadyExistsException e) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        }
    }
}
