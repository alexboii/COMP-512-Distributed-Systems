package flights;

import Constants.ServerConstants;
import RM.ResourceManager;

import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * Created by alex on 9/25/18.
 */
public class FlightsResourceManager extends ResourceManager implements IFlightsResourceManager {
    private static final String serverName = "Flights";

    public FlightsResourceManager() {
        super(serverName);
    }

    public static void main(String[] args) {
        // Figure out where server is running
        int port = 1099;

        if (args.length == 1) {
            port = Integer.parseInt(args[0]);
        } else if (args.length != 0 && args.length != 1) {
            System.err.println("Wrong usage");
            System.exit(1);
        }

        try {
            // Create a new server object and dynamically generate the stub (client proxy)
            FlightsResourceManager obj = new FlightsResourceManager();
            IFlightsResourceManager proxyObj = (IFlightsResourceManager) UnicastRemoteObject.exportObject(obj, 0);

            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry(port);
            registry.rebind(ServerConstants.FlightsPrefix, proxyObj);

            System.out.println("Flight server ready");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }

        // Create and install a security manager
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new RMISecurityManager());
        }
    }


    public boolean addFlights(int id, String location, int numFlights, int price) throws RemoteException {
        return addFlights(id, location, numFlights, price);
    }

    public boolean deleteFlights(int id, String location) throws RemoteException {
        return deleteFlights(id, location);
    }

    public int queryFlights(int id, String location) throws RemoteException {
        return queryFlights(id, location);
    }

    public int queryFlightsPrice(int id, String location) throws RemoteException {
        return queryFlightsPrice(id, location);
    }

    public boolean reserveFlight(int id, int customerID, String location) throws RemoteException {
        return reserveFlight(id, customerID, location);
    }

    public String getName() throws RemoteException {
        return getName();
    }
}
