package pt.tecnico.distledger.namingserver.domain;

public class ServerEntry {

    private String address;
    private String qualifier;

    public ServerEntry(String address, String qualifier){
        this.address = address;
        this.qualifier = qualifier;
    }

    public String getAddress(){
        return address;
    }

    public String getQualifier(){
        return qualifier;
    }
}