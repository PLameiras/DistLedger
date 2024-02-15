package pt.tecnico.distledger.namingserver.domain;

import pt.tecnico.distledger.namingserver.exceptions.CannotRegisterServerException;

import java.util.List;
import java.util.ArrayList;

public class ServiceEntry {

    private String service;
    private List<ServerEntry> serverEntries = new ArrayList<>();

    public ServiceEntry(String service){
        this.service = service;
    }

    public String getService() {
        return this.service;
    }

    public boolean checkServerEntryExists(ServerEntry newServer) {
        for(ServerEntry server : getServerEntries())
            if(server.getAddress().equals(newServer.getAddress()))
                return true;
        return false;
    }

    public void addServerEntry(ServerEntry serverEntry) throws CannotRegisterServerException {
        if(checkServerEntryExists(serverEntry)) throw new CannotRegisterServerException(this.getService(), serverEntry.getAddress(), serverEntry.getQualifier());
        serverEntries.add(serverEntry);
    }

    private void removeServerEntry(int index) {
        serverEntries.remove(index);
    }

    public List<ServerEntry> getServerEntries() {
        return serverEntries;
    }

    public void removeServerEntryWithAddress(String address) {
        List<ServerEntry> servers = getServerEntries();
        for(int i = 0; i < servers.size(); i++)
            if(servers.get(i).getAddress().equals(address))
                removeServerEntry(i);
    }
}
