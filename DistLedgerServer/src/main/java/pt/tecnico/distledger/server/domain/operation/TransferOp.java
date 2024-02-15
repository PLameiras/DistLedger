package pt.tecnico.distledger.server.domain.operation;

import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.exceptions.NoSuchAccountException;
import pt.tecnico.distledger.server.domain.exceptions.NotEnoughBalanceException;
import pt.tecnico.distledger.server.domain.exceptions.ServerInactiveException;
import pt.tecnico.distledger.server.grpc.Converter;

import java.util.List;

public class TransferOp extends Operation {
    private String destAccount;
    private int amount;

    public TransferOp(String fromAccount, String destAccount, int amount, List<Integer> prevTS, List<Integer> TS) {
        super(fromAccount, prevTS, TS);
        this.destAccount = destAccount;
        this.amount = amount;
    }

    public TransferOp(String fromAccount, String destAccount, int amount, List<Integer> prevTS) {
        super(fromAccount, prevTS);
        this.destAccount = destAccount;
        this.amount = amount;
    }

    public String getDestAccount() {
        return destAccount;
    }

    public void setDestAccount(String destAccount) {
        this.destAccount = destAccount;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    @Override
    public void accept(Converter visitor) {
        visitor.visit(this);
    }

    @Override
    public void execute(ServerState state) throws NoSuchAccountException, NotEnoughBalanceException, InterruptedException {
        state.execute(this);
    }

    @Override
    public String toString() {
        return "Transfer (" + this.getAmount() + " from account '" + this.getAccount() + "' to account '" + this.getDestAccount() + "')";
    }

    @Override
    public boolean equals(Object obj) {
        return obj.getClass() == this.getClass() && super.equals(obj) && this.getAmount() == ((TransferOp) obj).getAmount() && this.getDestAccount().equals(((TransferOp) obj).getDestAccount());
    }
}
