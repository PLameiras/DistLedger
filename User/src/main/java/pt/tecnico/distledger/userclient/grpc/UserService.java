package pt.tecnico.distledger.userclient.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.ulisboa.tecnico.distledger.contract.NamingServerServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.NamingServerServiceGrpc.NamingServerServiceBlockingStub;
import pt.ulisboa.tecnico.distledger.contract.NamingServer.LookupRequest;
import pt.ulisboa.tecnico.distledger.contract.NamingServer.LookupResponse;
import pt.ulisboa.tecnico.distledger.contract.NamingServer.ServerEntryResponse;
import pt.ulisboa.tecnico.distledger.contract.user.UserServiceGrpc;
import pt.ulisboa.tecnico.distledger.contract.user.UserServiceGrpc.UserServiceBlockingStub;
import io.grpc.StatusRuntimeException;
import pt.ulisboa.tecnico.distledger.contract.user.UserDistLedger.*;

import java.util.*;

public class UserService {

    private boolean debug;
    private Map<String, UserServiceBlockingStub> stubs;
    private NamingServerServiceBlockingStub namingServerStub;
    private TreeMap<String, Integer> previousTimeStamps;

    public UserService(String host, Integer port, boolean debug) {
        this.debug = debug;
        final String target = host + ":" + port;
        this.stubs = new HashMap<>();
        final ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        this.namingServerStub = NamingServerServiceGrpc.newBlockingStub(channel);
        initializePrevTS();
    }

    public boolean isDebug() {
        return this.debug;
    }

    private void initializePrevTS(){
        if(this.isDebug()) System.err.println("\tUserService: Building time stamp list");
        previousTimeStamps = new TreeMap<>();
        for (ServerEntryResponse server : this.lookup("DistLedger", "").getServersList()) {
            if(this.isDebug()) System.err.println("\tUserService: Building channel and stub for '" + server.getQualifier() + "'");
            if (!checkServerIsKnown(server.getQualifier())) {
                addStub(server.getQualifier(), UserServiceGrpc.newBlockingStub(ManagedChannelBuilder.forTarget(server.getAddress()).usePlaintext().build()));
                previousTimeStamps.put(server.getQualifier(), 0);
            }
        }
    }

    private void addTimeStamp(String key, Integer value) {
        previousTimeStamps.put(key, value);
    }

    private List<Integer> getTimeStampValues(){
        return new ArrayList<>(previousTimeStamps.values());
    }

    private List<String> getTimeStampKeys(){
        return new ArrayList<>(previousTimeStamps.keySet());
    }

    private void setTimeStamp(String key, Integer value){
        this.previousTimeStamps.put(key, value);
    }

    private void setTimeStampValues(List<Integer> timeStamps){
        Iterator<Integer> value = timeStamps.iterator();
        Iterator<String> key = this.getTimeStampKeys().iterator();
        while (value.hasNext() && key.hasNext()) setTimeStamp(key.next(), value.next());
    }

    private boolean checkServerIsKnown(String qualifier) {
        return stubs.containsKey(qualifier);
    }

    private void addStub(String qualifier, UserServiceBlockingStub stub) {
        stubs.put(qualifier, stub);
    }

    private UserServiceBlockingStub getStub(String qualifier) {
        return stubs.get(qualifier);
    }

    private NamingServerServiceBlockingStub getNamingServerStub() {
        return namingServerStub;
    }

    private Optional<UserServiceBlockingStub> buildChannelAndStub(String qualifier) {
        if (checkServerIsKnown(qualifier)) return getServerStub(qualifier);
        LookupResponse response = this.lookup("DistLedger", qualifier);
        if(response.getServersCount() == 0) return Optional.empty();
        if (this.isDebug()) System.err.println("\tUserService: Target server found at '" + response.getServers(0).getAddress() + "' " + timeStampsToString());
        if(this.isDebug()) System.err.println("\tUserService: Building channel and stub for '" + response.getServers(0).getQualifier() + "'");
        UserServiceBlockingStub stub;
        addStub(response.getServers(0).getQualifier(), (stub = UserServiceGrpc.newBlockingStub(ManagedChannelBuilder.forTarget(response.getServers(0).getAddress()).usePlaintext().build())));
        addTimeStamp(response.getServers(0).getQualifier(), 0);
        return Optional.ofNullable(stub);
    }

    private Optional<UserServiceBlockingStub> getServerStub(String qualifier) {
        if (checkServerIsKnown(qualifier)) return Optional.ofNullable(getStub(qualifier));
        else return buildChannelAndStub(qualifier);
    }

    public void createAccount(String qualifier, String userId) {
        CreateAccountResponse response;
        Optional<UserServiceBlockingStub> stub = getServerStub(qualifier);
        if(stub.isEmpty()) return;
        if(this.isDebug()) System.err.println("\tUserService: requesting create account of '" + userId + "' " + timeStampsToString());
        try {
            response = stub.get().createAccount(CreateAccountRequest.newBuilder().setUserId(userId).addAllPrevTS(this.getTimeStampValues()).build());
            this.setTimeStampValues(response.getTSList());
        } catch (StatusRuntimeException e) {
            System.out.println(e.getStatus().getCode());
            if (e.getStatus().getDescription() != null) System.out.println(e.getStatus().getDescription());
            return;
        }
        if(this.isDebug()) System.err.println("\tUserService: Time stamps after operation "  + timeStampsToString());
        System.out.println("OK\n");
    }

    public void balance(String qualifier, String userId) {
        BalanceResponse response;
        Optional<UserServiceBlockingStub> stub = getServerStub(qualifier);
        if(stub.isEmpty()) return;
        if(this.isDebug()) System.err.println("\tUserService: requesting balance of '" + userId + "' "  + timeStampsToString());
        try {
            response = stub.get().balance(BalanceRequest.newBuilder().setUserId(userId).addAllPrevTS(getTimeStampValues()).build());
            setTimeStampValues(response.getValueTSList());
        } catch (StatusRuntimeException e) {
            System.out.println(e.getStatus().getCode());
            if (e.getStatus().getDescription() != null) System.out.println(e.getStatus().getDescription());
            return;
        }
        if(this.isDebug()) System.err.println("\tUserService: Time stamps after operation "  + timeStampsToString());
        System.out.println("OK\n" + response.getValue());
    }

    public void transferTo(String qualifier, String accountFrom, String accountTo, Integer amount) {
        TransferToResponse response;
        Optional<UserServiceBlockingStub> stub = getServerStub(qualifier);
        if(stub.isEmpty()) return;
        if(this.isDebug()) System.err.println("\tUserService: requesting transfer to from '" + accountFrom + "' to '" + accountTo + "' of " + amount + " " + timeStampsToString());
        try {
            response = stub.get().transferTo(TransferToRequest.newBuilder().setAccountFrom(accountFrom).setAccountTo(accountTo).setAmount(amount).addAllPrevTS(getTimeStampValues()).build());
            setTimeStampValues(response.getTSList());
        } catch (StatusRuntimeException e) {
            System.out.println(e.getStatus().getCode());
            if (e.getStatus().getDescription() != null) System.out.println(e.getStatus().getDescription());
            return;
        }
        if(this.isDebug()) System.err.println("\tUserService: Time stamps after operation "  + timeStampsToString());
        System.out.println("OK\n");

    }

    public LookupResponse lookup(String service, String qualifier) {
        if(isDebug()) System.err.println("\tUserService: Proceeding to 'lookup' servers as '" + qualifier + "' on '" + service + "' "  + timeStampsToString());
        LookupResponse response = this.getNamingServerStub().lookup(LookupRequest.newBuilder().setService(service).setQualifier(qualifier).build());
        if(this.isDebug()) System.err.println("\tUserService: Received lookup response with " + response.getServersList().size() + " elements");
        return response;
    }

    public String timeStampsToString() {
        StringBuilder ts = new StringBuilder("(");
        for(Map.Entry<String, Integer> entry : previousTimeStamps.entrySet())
            ts.append(" <").append(entry.getKey()).append(", ").append(entry.getValue()).append(">");
        ts.append(" )");
        return ts.toString();
    }
}
