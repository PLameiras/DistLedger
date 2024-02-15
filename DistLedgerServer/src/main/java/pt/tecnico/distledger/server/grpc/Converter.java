package pt.tecnico.distledger.server.grpc;

import pt.tecnico.distledger.server.domain.operation.*;
import pt.ulisboa.tecnico.distledger.contract.DistLedgerCommonDefinitions;

import java.util.ArrayList;
import java.util.List;


public class Converter {

    ArrayList<DistLedgerCommonDefinitions.Operation> ledgerOperations = new ArrayList<>();
    public Converter() {}
    public Converter(Operation op) {
        this.addOperation(op);
    }
    public Converter(List<Operation> ops) {
        this.addOperations(ops);
    }

    public void addOperation(Operation op) {
        op.accept(this);
    }
    public void addOperations(List<Operation> ops) {
        for(Operation op : ops)
            addOperation(op);
    }
    public void visit(Operation op) {}
    public void visit(CreateOp op) {
        ledgerOperations.add(DistLedgerCommonDefinitions.Operation.newBuilder().setUserId(op.getAccount()).setType(DistLedgerCommonDefinitions.OperationType.OP_CREATE_ACCOUNT).addAllPrevTS(op.getPreviousTimeStamps()).addAllTS((op.getTimeStamps())).build());
    }
    public void visit(TransferOp op) {
        ledgerOperations.add(DistLedgerCommonDefinitions.Operation.newBuilder().setUserId(op.getAccount()).setDestUserId(op.getDestAccount()).setAmount(op.getAmount()).setType(DistLedgerCommonDefinitions.OperationType.OP_TRANSFER_TO).addAllPrevTS(op.getPreviousTimeStamps()).addAllTS((op.getTimeStamps())).build());
    }
    public List<DistLedgerCommonDefinitions.Operation> getLedgerOperations() {
        return ledgerOperations;
    }

}
