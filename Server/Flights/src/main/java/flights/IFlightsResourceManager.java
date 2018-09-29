package flights;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Created by alex on 9/25/18.
 */
public interface IFlightsResourceManager extends Remote {
    public boolean addFlights(int id, String location, int numFlights, int price)
            throws RemoteException;

    public boolean deleteFlights(int id, String location)
            throws RemoteException;

    public int queryFlights(int id, String location)
            throws RemoteException;

    public int queryFlightsPrice(int id, String location)
            throws RemoteException;

    public boolean reserveFlight(int id, int customerID, String location)
            throws RemoteException;

    public String getName()
            throws RemoteException;
}
