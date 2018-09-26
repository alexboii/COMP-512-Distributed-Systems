package cars;

import RM.ResourceManager;
import Constants.ServerConstants;

import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * Created by alex on 9/25/18.
 */
public class CarResourceManager extends ResourceManager implements ICarResourceManager {

    private static final String serverName = "Cars";

    public CarResourceManager() {
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
            CarResourceManager obj = new CarResourceManager();
            ICarResourceManager proxyObj = (ICarResourceManager) UnicastRemoteObject.exportObject(obj, 0);

            // Bind the remote object's stub in the registry
            Registry registry = LocateRegistry.getRegistry(port);
            registry.rebind(ServerConstants.CarPrefix, proxyObj);

            System.out.println("Car server ready");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }

        // Create and install a security manager
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new RMISecurityManager());
        }
    }


    public boolean addCars(int id, String location, int numCars, int price) throws RemoteException {
        return addCars(id, location, numCars, price);
    }

    public boolean deleteCars(int id, String location) throws RemoteException {
        return deleteCars(id, location);
    }

    public int queryCars(int id, String location) throws RemoteException {
        return queryCars(id, location);
    }

    public int queryCarsPrice(int id, String location) throws RemoteException {
        return queryCarsPrice(id, location);
    }

    public boolean reserveCar(int id, int customerID, String location) throws RemoteException {
        return reserveCar(id, customerID, location);
    }

    public String getName() throws RemoteException {
        return getName();
    }
}
