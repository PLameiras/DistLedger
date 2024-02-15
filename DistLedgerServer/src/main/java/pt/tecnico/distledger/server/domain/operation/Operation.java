package pt.tecnico.distledger.server.domain.operation;

import pt.tecnico.distledger.server.domain.ServerState;
import pt.tecnico.distledger.server.domain.exceptions.AccountAlreadyExistsException;
import pt.tecnico.distledger.server.domain.exceptions.NoSuchAccountException;
import pt.tecnico.distledger.server.domain.exceptions.NotEnoughBalanceException;
import pt.tecnico.distledger.server.domain.exceptions.ServerInactiveException;
import pt.tecnico.distledger.server.grpc.Converter;

import java.util.*;

public class Operation {
    private String account;
    List<Integer> previousTimeStamps;
    List<Integer> timeStamps;
    boolean stable = false;

    public Operation(String fromAccount, List<Integer> prevTS, List<Integer> TS) {
        this(fromAccount, prevTS);
        this.timeStamps = new ArrayList<>(TS);
    }

    public Operation(String fromAccount, List<Integer> prevTS) {
        this.account = fromAccount;
        this.previousTimeStamps = new ArrayList<>(prevTS);
        this.timeStamps = null;
    }

    public String getAccount() {
        return account;
    }

    public List<Integer> getPreviousTimeStamps() {
        return Collections.unmodifiableList(previousTimeStamps);
    }

    public List<Integer> getTimeStamps() {
        return Collections.unmodifiableList(timeStamps);
    }

    public void setTimeStamps(List<Integer> TS) {
        this.timeStamps = new ArrayList<>(TS);
    }

    public void setTimeStamp(int index, Integer value) {
        if(this.timeStamps.size() - 1 < index) this.timeStamps.add(value);
        else this.timeStamps.set(index, value);
    }

    public void setStable() {
        this.stable = true;
    }

    public boolean isStable() {return stable;}

    public void accept(Converter opConverter) {
        opConverter.visit(this);
    }

    public void execute(ServerState state) throws AccountAlreadyExistsException, NoSuchAccountException, NotEnoughBalanceException, InterruptedException  {}

    @Override
    public boolean equals(Object obj) {
        if(obj == null || obj.getClass() != this.getClass()) return false;
        Iterator<Integer> c = (this.getPreviousTimeStamps() == null) ? null : this.getPreviousTimeStamps().iterator(), cOp = (((Operation) obj).getPreviousTimeStamps() == null) ? null : ((Operation) obj).getPreviousTimeStamps().iterator();
        if(c != null && cOp != null) {
            while (c.hasNext() && cOp.hasNext()) if (!Objects.equals(c.next(), cOp.next())) return false;
            if (c.hasNext() ^ cOp.hasNext()) return false;
        } else if (c == null ^ cOp == null) return false;
        return this.getAccount().equals(((Operation) obj).getAccount());
    }
}
