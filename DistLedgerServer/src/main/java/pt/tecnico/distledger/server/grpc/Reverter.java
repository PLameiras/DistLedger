package pt.tecnico.distledger.server.grpc;

import pt.tecnico.distledger.server.domain.operation.*;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions;

import java.util.ArrayList;
import java.util.List;


public class Reverter {

    ArrayList<Operation> ledgerOperations;
    public Reverter() {
        this.ledgerOperations = new ArrayList<>();
    }
    public Reverter(DistLedgerCommonDefinitions.Operation op) {
        this();
        this.addOperation(op);
    }
    public Reverter(List<DistLedgerCommonDefinitions.Operation> ops) {
        this();
        this.addOperations(ops);
    }

    public void addOperation(DistLedgerCommonDefinitions.Operation op) {
        switch(op.getType()) {
            case OP_UNSPECIFIED:
                ledgerOperations.add(new Operation(op.getUserId(), op.getPrevTSList(), op.getTSList()));
                break;
            case OP_CREATE_ACCOUNT:
                ledgerOperations.add(new CreateOp(op.getUserId(), op.getPrevTSList(), op.getTSList()));;
                break;
            case OP_TRANSFER_TO:
                ledgerOperations.add(new TransferOp(op.getUserId(), op.getDestUserId(), op.getAmount(), op.getPrevTSList(), op.getTSList()));
                break;
        }
    }
    public void addOperations(List<DistLedgerCommonDefinitions.Operation> ops) {
        for(DistLedgerCommonDefinitions.Operation operation : ops)
            this.addOperation(operation);
    }
    public List<Operation> getLedgerOperations() {
        return ledgerOperations;
    }

}