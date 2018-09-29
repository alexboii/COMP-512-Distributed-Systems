package rooms;

import Constants.ServerConstants;
import RM.IResourceManager;
import RM.ResourceManager;

import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * Created by alex on 9/25/18.
 */
public class RoomsResourceManager extends ResourceManager {
    private static final String serverName = "Rooms";

    public RoomsResourceManager() {
        super(serverName);
    }

    public static void main(String[] args) {
        // Figure out where server is running
        int port = ServerConstants.ROOMS_SERVER_PORT;

        if (args.length == 1) {
            port = Integer.parseInt(args[0]);
        } else if (args.length != 0 && args.length != 1) {
            System.err.println("Wrong usage");
            System.exit(1);
        }

        try {
            // Create a new server object and dynamically generate the stub (client proxy)
            RoomsResourceManager obj = new RoomsResourceManager();
            IResourceManager resourceManager = (IResourceManager) UnicastRemoteObject.exportObject(obj, 0);

            // Bind the remote object's stub in the registry
            Registry l_registry;
            try {
                l_registry = LocateRegistry.createRegistry(port);
            } catch (RemoteException e) {
                l_registry = LocateRegistry.getRegistry(port);
            }
            final Registry registry = l_registry;
            registry.rebind(ServerConstants.ROOMS_PREFIX, resourceManager);

            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    try {
                        registry.unbind(ServerConstants.ROOMS_PREFIX);
                        System.out.println("'" + ServerConstants.ROOMS_PREFIX + "' resource manager unbound");
                    }
                    catch(Exception e) {
                        System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
                        e.printStackTrace();
                    }
                }
            });
            System.out.println("'" + serverName + "' resource manager server ready and bound to '" + ServerConstants.ROOMS_PREFIX + "'");

            System.out.println("Rooms server ready");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }

        // Create and install a security manager
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new RMISecurityManager());
        }
    }
}
