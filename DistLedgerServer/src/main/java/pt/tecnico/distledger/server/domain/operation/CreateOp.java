package pt.tecnico.distledger.server.domain.operation;

import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.exceptions.AccountAlreadyExistsException;
import pt.tecnico.distledger.server.domain.exceptions.ServerInactiveException;
import pt.tecnico.distledger.server.grpc.Converter;

import java.util.List;

public class CreateOp extends Operation {

    public CreateOp(String account, List<Integer> prevTS, List<Integer> TS) {
        super(account, prevTS, TS);
    }

    public CreateOp(String account, List<Integer> prevTS) {
        super(account, prevTS);
    }

    @Override
    public void accept(Converter visitor) {
        visitor.visit(this);
    }

    @Override
    public void execute(ServerState state) throws AccountAlreadyExistsException {
        state.execute(this);
    }

    @Override
    public String toString() {
        return "Create (account '" + this.getAccount() + "')";
    }

    @Override
    public boolean equals(Object obj) {
        return obj.getClass() == this.getClass() && super.equals(obj);
    }
}
