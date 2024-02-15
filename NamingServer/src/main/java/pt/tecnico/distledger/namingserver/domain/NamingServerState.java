package pt.tecnico.distledger.namingserver.domain;

import pt.tecnico.distledger.namingserver.exceptions.*;
import java.util.stream.Collectors;
import java.util.*;

public class NamingServerState {

    private boolean debug;

    Map<String, ServiceEntry> services;

    public NamingServerState() {
        this.debug = false;
        services = new HashMap<>();
    }

    public NamingServerState(boolean debug) {
        this();
        this.debug = debug;
    }

    public boolean isDebug() {
        return this.debug;
    }

    private ServiceEntry getServiceEntry(String service) {
        return this.services.get(service);
    }

    private boolean checkServiceExists(String service) {
        return services.containsKey(service);
    }

    private void addServiceEntry(String service) {
        if(!this.checkServiceExists(service)) {
            if(this.isDebug()) System.err.println("\t\t\tNamingServerState: Creating '" + service + "' service.");
            this.services.put(service, (new ServiceEntry(service)));
        }
        else {
            if(this.isDebug()) System.err.println("\t\t\tNamingServerState: '" + service + "' already exists.");
        }
    }

    private List<ServerEntry> getServiceServers(String service) {
        return this.checkServiceExists(service) ? getServiceEntry(service).getServerEntries() : new ArrayList<>();
    }


    public void register (String address, String qualifier, String service) throws CannotRegisterServerException {
        if(!this.checkServiceExists(service))
            this.addServiceEntry(service);
        if(this.isDebug()) System.err.println("\t\t\tNamingServerState: Adding '" + qualifier + "' at '" + address + "' to '" + service + "' service");
        this.getServiceEntry(service).addServerEntry(new ServerEntry(address, qualifier));
    }

    public List<ServerEntry> lookup (String service, String qualifier) {
        List<ServerEntry> s = getServiceServers(service);
        if(this.isDebug()) System.err.println("\t\t\tNamingServerState: Found " + s.size() + " servers for service '" + service + "'");
        if (s.size() == 0 || qualifier.equals("")) return s;
        s = s.stream().filter(e -> e.getQualifier().equals(qualifier)).collect(Collectors.toList());
        if(this.isDebug()) System.err.println("\t\t\tNamingServerState: Found " + s.size() + " matching servers for qualifier '" + qualifier + "'");
        return s;
    }

    public void delete (String address, String service) throws CannotRemoveServerException {
        if(this.checkServiceExists(service)){
            if(this.isDebug()) System.err.println("\t\t\tNamingServerState: Deleting server at '" + address + "'");
            this.getServiceEntry(service).removeServerEntryWithAddress(address);
        }
        else throw new CannotRemoveServerException(service, address);
    }
}