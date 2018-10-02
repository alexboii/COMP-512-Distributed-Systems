package middleware;

import Constants.ServerConstants;
import RM.IResourceManager;
import RM.ResourceManager;
import customer.CustomerResourceManager;

import java.rmi.NotBoundException;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Vector;

public class Middleware extends ResourceManager {
    private static final String serverName = "Middleware";

    public static void main(String[] args) {

        // Figure out where server is running
        int port = 1088;

        if (args.length == 1) {
            port = Integer.parseInt(args[0]);
        } else if (args.length != 0 && args.length != 1) {
            System.err.println("Wrong usage");
            System.exit(1);
        }

        try {
            Middleware obj = new Middleware();

            // Create a new server object and dynamically generate the stub (client proxy)
            IResourceManager resourceManager = (IResourceManager) UnicastRemoteObject.exportObject(obj, 0);

            // Bind the remote object's stub in the registry
            // Bind the remote object's stub in the registry
            Registry l_registry;
            try {
                l_registry = LocateRegistry.createRegistry(port);
            } catch (RemoteException e) {
                l_registry = LocateRegistry.getRegistry(port);
            }
            final Registry registry = l_registry;
            registry.rebind(ServerConstants.MIDDLEWARE_PREFIX, resourceManager);

            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    try {
                        registry.unbind(ServerConstants.MIDDLEWARE_PREFIX);
                        System.out.println("'" + ServerConstants.MIDDLEWARE_PREFIX + "' resource manager unbound");
                    } catch (Exception e) {
                        System.err.println((char) 27 + "[31;1mServer exception: " + (char) 27 + "[0mUncaught exception");
                        e.printStackTrace();
                    }
                }
            });
            System.out.println("'middleware' resource manager server ready and bound to '" + ServerConstants.MIDDLEWARE_PREFIX + port);

            System.out.println("Middleware server ready");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }

        // Create and install a security manager
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new RMISecurityManager());
        }
    }

    public IResourceManager carsManager;
    public IResourceManager flightsManager;
    public IResourceManager roomsManager;
    public CustomerResourceManager customerManager;

    public Middleware() {
        super("middleware");
        carsManager = connectServer(ServerConstants.CAR_SERVER_NAME, ServerConstants.CAR_SERVER_PORT, ServerConstants.CAR_PREFIX);
        roomsManager = connectServer(ServerConstants.ROOMS_SERVER, ServerConstants.ROOMS_SERVER_PORT, ServerConstants.ROOMS_PREFIX);
        flightsManager = connectServer(ServerConstants.FLIGHTS_SERVER_NAME, ServerConstants.FLIGHTS_SERVER_PORT, ServerConstants.FLIGHTS_PREFIX);
        customerManager = new CustomerResourceManager();
    }

    @Override
    public boolean addFlight(int id, int flightNum, int flightSeats, int flightPrice) throws RemoteException {
        return customerManager.addFlight(id, flightNum, flightSeats, flightPrice) && flightsManager.addFlight(id, flightNum, flightSeats, flightPrice);
    }

    @Override
    public boolean addCars(int id, String location, int numCars, int price) throws RemoteException {
        return carsManager.addCars(id, location, numCars, price);
    }

    @Override
    public boolean addRooms(int id, String location, int numRooms, int price) throws RemoteException {
        return roomsManager.addRooms(id, location, numRooms, price);
    }

    @Override
    public int newCustomer(int id) throws RemoteException {
        return customerManager.newCustomer(id) == 1 && flightsManager.newCustomer(id) == 1 && roomsManager.newCustomer(id) == 1 && carsManager.newCustomer(id) == 1 ? 1 : 0;
    }

    @Override
    public boolean newCustomer(int id, int cid) throws RemoteException {
        return customerManager.newCustomer(id, cid) && flightsManager.newCustomer(id, cid) && roomsManager.newCustomer(id, cid) && carsManager.newCustomer(id, cid);
    }

    @Override
    public boolean deleteFlight(int id, int flightNum) throws RemoteException {
        return flightsManager.deleteFlight(id, flightNum);
    }

    @Override
    public boolean deleteCars(int id, String location) throws RemoteException {
        return carsManager.deleteCars(id, location);
    }

    @Override
    public boolean deleteRooms(int id, String location) throws RemoteException {
        return roomsManager.deleteRooms(id, location);
    }

    @Override
    public boolean deleteCustomer(int id, int customerID) throws RemoteException {
        return customerManager.deleteCustomer(id, customerID) && flightsManager.deleteCustomer(id, customerID) && roomsManager.deleteCustomer(id, customerID) && carsManager.deleteCustomer(id, customerID);
    }

    @Override
    public int queryFlight(int id, int flightNumber) throws RemoteException {
        return flightsManager.queryFlight(id, flightNumber);
    }

    @Override
    public int queryCars(int id, String location) throws RemoteException {
        return carsManager.queryCars(id, location);
    }

    @Override
    public int queryRooms(int id, String location) throws RemoteException {
        return roomsManager.queryRooms(id, location);
    }

    @Override
    public String queryCustomerInfo(int id, int customerID) throws RemoteException {
        return customerManager.queryCustomerInfo(id, customerID);
    }

    @Override
    public int queryFlightPrice(int id, int flightNumber) throws RemoteException {
        return flightsManager.queryFlightPrice(id, flightNumber);
    }

    @Override
    public int queryCarsPrice(int id, String location) throws RemoteException {
        return carsManager.queryCarsPrice(id, location);
    }

    @Override
    public int queryRoomsPrice(int id, String location) throws RemoteException {
        return roomsManager.queryRoomsPrice(id, location);
    }

    @Override
    public boolean reserveFlight(int id, int customerID, int flightNumber) throws RemoteException {
        if (!flightsManager.reserveFlight(id, customerID, flightNumber)) {
            return false;
        }

        int price = queryFlightPrice(id, flightNumber);

        return customerManager.reserveFlight(id, customerID, flightNumber, price);
    }

    @Override
    public boolean reserveCar(int id, int customerID, String location) throws RemoteException {
        if (!carsManager.reserveCar(id, customerID, location)) {
            return false;
        }

        int price = queryCarsPrice(id, location);

        return customerManager.reserveCar(id, customerID, location, price);
    }

    @Override
    public boolean reserveRoom(int id, int customerID, String location) throws RemoteException {

        if (!roomsManager.reserveRoom(id, customerID, location)) {
            return false;
        }

        int price = queryRoomsPrice(id, location);

        return customerManager.reserveRoom(id, customerID, location, price);
    }

    @Override
    public boolean bundle(int id, int customerID, Vector<String> flightNumbers, String location, boolean car, boolean room) throws RemoteException {
        for(String flightNumber : flightNumbers){
            try {
                int parsedFlightNumber = Integer.parseInt(flightNumber);

                // TODO: DEAL WITH THE CASE OF CORRUPTED DATA IF ONE CALL FAILS AND THE OTHERS GO THROUGH IN THE NEXT ITERATION, NOT NOW THOUGH
                // it was said by TA in Discussion forum of myCourses that we do not need to deal with corrupt data for now, so it's okay
                // TODO: WILL ALSO HAVE TO IMPLEMENT "UNRESERVE" METHODS FOR THIS

                if(!reserveFlight(id, customerID, parsedFlightNumber)){
                    return false;
                }

            } catch(NumberFormatException e){
                System.out.println(e);
                return false;
            }
        }

        if(car && !reserveCar(id, customerID, location)){
            return false;
        }

        return room ? reserveRoom(id, customerID, location) : true;
    }

    @Override
    public String getName() throws RemoteException {
        return null;
    }

    public IResourceManager connectServer(String address, int port, String prefix) {

        IResourceManager m_resourceManager = null;

        try {
            boolean first = true;
            while (true) {

                try {
                    Registry registry = LocateRegistry.getRegistry(address, port);
                    m_resourceManager = (IResourceManager) registry.lookup(prefix);
                    System.out.println("Connected to '" + prefix + "' server [" + address + ":" + port + "/" + prefix + "]");

                    return m_resourceManager;
                } catch (NotBoundException | RemoteException e) {
                    if (first) {
                        System.out.println("Waiting for '" + prefix + "' server [" + address + ":" + port + "/" + prefix + "]");
                        first = false;
                    }
                }
                Thread.sleep(500);
            }
        } catch (Exception e) {
            System.err.println((char) 27 + "[31;1mServer exception: " + (char) 27 + "[0mUncaught exception");
            e.printStackTrace();
            System.exit(1);
        }

        return m_resourceManager;
    }
}
