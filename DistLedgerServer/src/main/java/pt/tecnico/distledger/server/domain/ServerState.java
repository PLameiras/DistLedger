package pt.tecnico.distledger.server.domain;

import pt.tecnico.distledger.server.domain.operation.Operation;
import pt.tecnico.distledger.server.domain.operation.CreateOp;
import pt.tecnico.distledger.server.domain.operation.TransferOp;
import pt.tecnico.distledger.server.grpc.ServerService;
import pt.tecnico.distledger.server.domain.exceptions.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ServerState {
    private enum ServerCurrentState {
        ACTIVE,
        INACTIVE
    }

    private final List<Operation> ledger = new ArrayList<>();
    private final List<Operation> unstableOps = new ArrayList<>();
    private final List<Operation> opsNotPropagated = new ArrayList<>();
    Map<String, Integer> accounts = new ConcurrentHashMap<>();
    private final TreeMap<String, Integer> valueTS = new TreeMap<>();
    private final TreeMap<String, Integer> replicaTS = new TreeMap<>();
    private final boolean debug;
    private ServerCurrentState state;
    private ServerService serverService;

    public ServerState(ServerService serverService) {
        this.setServerService(serverService);
        this.debug = serverService.isDebug();
        this.addAccount("broker", 1000);
        this.addTimeStamp(this.getServerQualifier());
        this.register();
        this.initialize();
        this.state = ServerCurrentState.ACTIVE;
    }

    private void initialize() {
        if(this.isDebug()) System.out.println("\t\tServerState: initializing time stamps");
        getServerService().initialize();
    }

    private void setServerService(ServerService service) {
        this.serverService = service;
        service.setState(this);
    }

    public ServerService getServerService() {
        return this.serverService;
    }

    public String getService() {
        return this.getServerService().getServerService();
    }

    public String getServerAddress() {
        return this.getServerService().getServerAddress();
    }

    public String getServerPort() {
        return this.getServerService().getServerPort();
    }

    public String getServerQualifier() {
        return this.getServerService().getServerQualifier();
    }

    public String getSelf() {
        return getServerAddress() + ":" + getServerPort();
    }

    public boolean isDebug() {
        return this.debug;
    }

    private ServerCurrentState getState() {
        return this.state;
    }

    private void setState(ServerCurrentState state) {
        this.state = state;
    }

    private Map<String, Integer> getTimeStamp() {
        return Collections.unmodifiableMap(valueTS);
    }

    private synchronized void setValueTS(String key, Integer value) {
        valueTS.put(key, value);
        notifyAll();
    }

    private void setReplicaTS(String key, Integer value) {
        replicaTS.put(key, value);
    }

    private Map<String, Integer> getReplicaTimeStamp() {
        return Collections.unmodifiableMap(replicaTS);
    }

    private List<Operation> getUnstableOperations() {
        return Collections.unmodifiableList(this.unstableOps);
    }

    private Optional<Operation> getExecutableUnstableOperation() {
        for(Operation operation : this.getUnstableOperations()) if(checkOperationExecutable(operation)) return Optional.ofNullable(operation);
        return Optional.empty();
    }

    private void removeUnstableOperation(Operation operation) {
        this.unstableOps.remove(operation);
    }

    private boolean checkServerActive() {
        return this.getState() == ServerCurrentState.ACTIVE;
    }

    public void addTimeStamp(String qualifier) {
        if(!this.getTimeStamp().containsKey(qualifier)) {
            this.valueTS.put(qualifier, 0);
            this.replicaTS.put(qualifier, 0);
        }
    }

    public List<Integer> getTimeStampValues(){
        return new ArrayList<>(getTimeStamp().values());
    }

    public List<Integer> getReplicaTimeStampValues(){
        return new ArrayList<>(getReplicaTimeStamp().values());
    }

    private List<String> getTimeStampKeys(){
        return new ArrayList<>(getTimeStamp().keySet());
    }

    private Integer getTimeStampIndex(String key){
        return getTimeStampKeys().indexOf(key);
    }

    private void setValueTSValues(List<Integer> values) {
        if(this.getTimeStamp().size() != values.size()) return;
        Iterator<Integer> v = values.iterator();
        Iterator<String> k = this.valueTS.keySet().iterator();
        while (v.hasNext() && k.hasNext()) {
            this.setValueTS(k.next(), v.next());
        }
    }

    private void setReplicaTSValues(List<Integer> values) {
        if(this.getTimeStamp().size() != values.size()) return;
        Iterator<Integer> v = values.iterator();
        Iterator<String> k = this.replicaTS.keySet().iterator();
        while (v.hasNext() && k.hasNext()) {
            this.setReplicaTS(k.next(), v.next());
        }
    }

    // Vb=max(Va,Vb) for all entries:
    private void mergeTimeStampValue(List<Integer> receivedTimeStampValues) {
        Optional<List<Integer>> merge = this.mergeList(receivedTimeStampValues, this.getTimeStampValues());
        merge.ifPresent(this::setValueTSValues);
    }

    // Vb=max(Va,Vb) for all entries:
    private void mergeTimeStampReplica(List<Integer> receivedTimeStampValues) {
        Optional<List<Integer>> merge = this.mergeList(receivedTimeStampValues, this.getReplicaTimeStampValues());
        merge.ifPresent(this::setReplicaTSValues);
    }

    // Vb=max(Va,Vb) for all entries:
    private Optional<List<Integer>> mergeList(List<Integer> first, List<Integer> second) {
        if(first.size() != second.size()) return Optional.empty();
        ArrayList<Integer> result = new ArrayList<>();
        int f, s;
        for (Iterator<Integer> firstValues = second.iterator(), secondValues = first.iterator(); firstValues.hasNext() && secondValues.hasNext();)
            result.add(((f = firstValues.next()) > (s = secondValues.next())) ? f : s);
        return Optional.of(result);
    }

    private Integer incrementReplicaTimeStamp() {
        replicaTS.put(this.getServerQualifier(), replicaTS.get(this.getServerQualifier()) + 1);
        return replicaTS.get(this.getServerQualifier());
    }

    private List<Operation> getOperations() {
        return this.ledger;
    }

    private void addOperation(Operation operation) {
        ledger.add(operation);
    }

    private List<Operation> getOperationsNotPropagated() {
        return Collections.unmodifiableList(opsNotPropagated);
    }

    private void addOperationNotPropagated(Operation operation) {
        opsNotPropagated.add(operation);
    }

    private void clearOperationsNotPropagated() {
        opsNotPropagated.clear();
    }

    private void addUnstableOp(Operation operation) {
        if(!this.checkOperationRegistrable(operation)) {
            if(isDebug()) System.err.println("\t\tServerState: Operation not registrable");
            return;
        }
        if(isDebug()) System.err.println("\t\tServerState: Registering as unstable " + operation);
        for (int i = 0; i < unstableOps.size(); i++)
            if(checkCasualOrder(unstableOps.get(i).getTimeStamps(), operation.getPreviousTimeStamps())) {
                unstableOps.add(i, operation);
                return;
            }
        unstableOps.add(operation);
    }

    private void addAllUnstableOp(List<Operation> operations) {
        for(Operation op : operations) addUnstableOp(op);
    }

    private void addAccount(String userId, int amount) {
        this.accounts.put(userId, amount);
    }

    private int getBalance(String userId) {
        return this.accounts.get(userId);
    }

    private void setBalance(String userId, int amount) {
        this.accounts.put(userId, amount);
    }

    private boolean checkAccountExists(String userId) {
        return this.accounts.containsKey(userId);
    }

    private boolean checkEnoughBalance(String account, int amount) {
        return (this.accounts.get(account) >= amount);
    }

    private boolean checkCasualOrder(List<Integer> TS1, List<Integer> TS2){
        for(Iterator<Integer> iteratorTS1 = TS1.iterator(), iteratorTS2 = TS2.iterator(); iteratorTS1.hasNext() && iteratorTS2.hasNext();)
            if(iteratorTS1.next() < iteratorTS2.next()) return false;
        return true;
    }

    private boolean checkOperationExecutable(Operation op){
        if(isDebug()) System.err.println("\t\tServerState: Comparing prevTS " + this.vectorClockToString(op.getPreviousTimeStamps()) + " to valueTS " + this.vectorClockToString(this.getTimeStampValues()));
        Iterator<Integer> valueTS = this.getTimeStampValues().iterator(), prevTS = op.getPreviousTimeStamps().iterator();
        while(valueTS.hasNext() && prevTS.hasNext()) if(valueTS.next() < prevTS.next()) return false;
        return valueTS.hasNext() == prevTS.hasNext();
    }

    // Each record r from the state received in receiveState() is only added to the log when replicaTS < r.ts, avoiding duplicate operations
    private boolean checkOperationRegistrable(Operation op){
        if(isDebug()) System.err.println("\t\tServerState: Comparing operationTS " + this.vectorClockToString(op.getTimeStamps()) + " to replicaTS " + this.vectorClockToString(this.getReplicaTimeStampValues()));
        Iterator<Integer> replicaTS = this.getReplicaTimeStampValues().iterator(), operationTS = op.getTimeStamps().iterator();
        while(replicaTS.hasNext() && operationTS.hasNext()) if(replicaTS.next() < operationTS.next()) return true;
        return operationTS.hasNext();
    }

    public synchronized void createAccount(CreateOp op) throws AccountAlreadyExistsException {
        if (this.checkAccountExists(op.getAccount())) throw new AccountAlreadyExistsException(op.getAccount());

        if(isDebug()) System.err.println("\t\tServerState: Creating account '" + op.getAccount() + "' " + timeStampToString());

        addAccount(op.getAccount(), 0);

        addOperation(op);
        op.setStable();

        if(isDebug()) System.err.println("\t\tServerState: Account creation successful");
    }

    public synchronized int balance(Operation op) throws NoSuchAccountException {
        if (!this.checkAccountExists(op.getAccount())) throw new NoSuchAccountException(op.getAccount());
        if(isDebug()) System.err.println("\t\tServerState: Getting balance of '" + op.getAccount() + "'");
        while(!checkOperationExecutable(op)){
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Thread Interrupted\n");
            }
        }
        int balance = getBalance(op.getAccount());
        if(isDebug()) System.err.println("\t\tServerState: Balance of '" + op.getAccount() + "' is + " + balance);
        return balance;
    }

    public synchronized void transferTo(TransferOp op) throws NoSuchAccountException, NotEnoughBalanceException {
        if (!this.checkAccountExists(op.getAccount())) throw new NoSuchAccountException(op.getAccount());
        if (!this.checkAccountExists(op.getDestAccount())) throw new NoSuchAccountException(op.getDestAccount());
        if (!checkEnoughBalance(op.getAccount(), op.getAmount())) throw new NotEnoughBalanceException(op.getAccount());

        if(isDebug()) System.err.println("\t\tServerState: transferring " + op.getAmount() + " from '" + op.getAccount() + "' to '" + op.getDestAccount() + "' " + timeStampToString());

        setBalance(op.getAccount(), getBalance(op.getAccount()) - op.getAmount());
        setBalance(op.getDestAccount(), getBalance(op.getDestAccount()) + op.getAmount());

        addOperation(op);
        op.setStable();

        if(isDebug()) System.err.println("\t\tServerState: Transfer successful");
    }

    public synchronized void activate() throws ServerAlreadyActiveException {
        if (this.checkServerActive()) throw new ServerAlreadyActiveException();
        if(isDebug()) System.err.println("\t\tServerState: Activating server");
        this.setState(ServerCurrentState.ACTIVE);
    }

    public synchronized void deactivate() throws ServerAlreadyInactiveException{
        if (!this.checkServerActive()) throw new ServerAlreadyInactiveException();
        if(isDebug()) System.err.println("\t\tServerState: Deactivating server");
        this.setState(ServerCurrentState.INACTIVE);
    }

    public synchronized List<Operation> getLedgerState() throws ServerInactiveException {
        if (!this.checkServerActive()) throw new ServerInactiveException();
        if(isDebug()) System.err.println("\t\tServerState: Getting ledger");
        return Collections.unmodifiableList(this.getOperations());
    }

    private synchronized void preprocessUpdate(Operation op) throws ServerInactiveException {
        if(!checkServerActive()) throw new ServerInactiveException();
        op.setTimeStamps(op.getPreviousTimeStamps());
        op.setTimeStamp(getTimeStampIndex(this.getServerQualifier()), incrementReplicaTimeStamp());
    }

    public void executeUpdate(Operation op) throws AccountAlreadyExistsException, NoSuchAccountException, NotEnoughBalanceException, ServerInactiveException, InterruptedException {
        preprocessUpdate(op);
        if(checkOperationExecutable(op)) {
            if(isDebug()) System.err.println("\t\tServerState: Executing " + op);
            op.execute(this);
            if(op.isStable()) addOperationNotPropagated(op);
        } else {
            if(isDebug()) System.err.println("\t\tServerState: Operation not executable");
            this.addUnstableOp(op);
            addOperationNotPropagated(op);
        }
    }

    public int executeQuery(Operation op) throws NoSuchAccountException, ServerInactiveException, InterruptedException {
        return balance(op);
    }

    public void execute(CreateOp op) throws AccountAlreadyExistsException {
        try {
            this.createAccount(op);
        } catch(AccountAlreadyExistsException e) {
            mergeTimeStampValue(op.getTimeStamps());
            throw new AccountAlreadyExistsException(op.getAccount());
        }
        mergeTimeStampValue(op.getTimeStamps());
    }

    public void execute(TransferOp op) throws NoSuchAccountException, NotEnoughBalanceException {
        try {
            this.transferTo(op);
        } catch (NoSuchAccountException e) {
            mergeTimeStampValue(op.getTimeStamps());
            throw new NoSuchAccountException(op.getAccount());
        } catch (NotEnoughBalanceException e) {
            mergeTimeStampValue(op.getTimeStamps());
            throw new NotEnoughBalanceException(op.getAccount());
        }
        mergeTimeStampValue(op.getTimeStamps());
    }

    public synchronized void receiveState(List<Operation> operationLog, List<Integer> timeStamp) throws ServerInactiveException, InterruptedException {
        if(!this.checkServerActive()) throw new ServerInactiveException(true);
        Operation operation;
        if(isDebug()) System.err.println("\t\tServerState: 'receivedState' " + operationLog.size() + " operations to execute");
        this.addAllUnstableOp(operationLog);
        this.mergeTimeStampReplica(timeStamp);
        for(Optional<Operation> op; (operation = (op = this.getExecutableUnstableOperation()).isPresent() ? op.get() : null) != null; this.removeUnstableOperation(operation)) {
            if(isDebug()) System.err.println("\t\t-> Executing unstable: " + operation);
            try {
                operation.execute(this);
            } catch (AccountAlreadyExistsException | NoSuchAccountException |NotEnoughBalanceException e) {
                if(isDebug()) System.err.println("\t\t\t-> Caught: " + e.getMessage());
            }
        }
        if(isDebug()) System.err.println("\t\tServerState: No more unstable operations can be executed");
    }

    private synchronized void register() {
        try {
            this.getServerService().register();
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    public synchronized List<Operation> gossip() throws ServerInactiveException, NoSecondaryServerException {
        if(!this.checkServerActive()) throw new ServerInactiveException(true);
        List<Operation> operations = new ArrayList<>(this.getOperationsNotPropagated());
        if(isDebug()) System.err.println("\t\tServerState: 'gossip' propagating " + this.getOperationsNotPropagated().size() + " operations " + timeStampToString());
        this.clearOperationsNotPropagated();
        return operations;
    }

    public synchronized void delete() {
        try {
            this.getServerService().delete();
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    public String timeStampToString() {
        StringBuilder ts = new StringBuilder("(");
        for(Map.Entry<String, Integer> entry : getTimeStamp().entrySet())
            ts.append(" <").append(entry.getKey()).append(", ").append(entry.getValue()).append(">");
        ts.append(" )");
        return ts.toString();
    }

    public String vectorClockToString(List<Integer> vector) {
        StringBuilder ts = new StringBuilder("(");
        for(Integer value : vector)
            ts.append(" <").append(value).append(">");
        ts.append(" )");
        return ts.toString();
    }
}