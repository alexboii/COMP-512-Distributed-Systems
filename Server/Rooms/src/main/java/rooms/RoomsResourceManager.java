package rooms;

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
public class RoomsResourceManager extends ResourceManager implements IRoomsResourceManager {
    private static final String serverName = "Rooms";

    public RoomsResourceManager() {
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
            RoomsResourceManager obj = new RoomsResourceManager();
            IRoomsResourceManager proxyObj = (IRoomsResourceManager) UnicastRemoteObject.exportObject(obj, 0);

            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry(port);
            registry.rebind(ServerConstants.RoomsPrefix, proxyObj);

            System.out.println("Room server ready");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }

        // Create and install a security manager
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new RMISecurityManager());
        }
    }


    public boolean addRooms(int id, String location, int numRooms, int price) throws RemoteException {
        return addRooms(id, location, numRooms, price);
    }

    public boolean deleteRooms(int id, String location) throws RemoteException {
        return deleteRooms(id, location);
    }

    public int queryRooms(int id, String location) throws RemoteException {
        return queryRooms(id, location);
    }

    public int queryRoomsPrice(int id, String location) throws RemoteException {
        return queryRoomsPrice(id, location);
    }

    public boolean reserveRoom(int id, int customerID, String location) throws RemoteException {
        return reserveRoom(id, customerID, location);
    }

    public String getName() throws RemoteException {
        return getName();
    }
}
