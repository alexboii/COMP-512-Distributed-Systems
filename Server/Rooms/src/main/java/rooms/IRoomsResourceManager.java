package rooms;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Created by alex on 9/25/18.
 */
public interface IRoomsResourceManager extends Remote {
    public boolean addRooms(int id, String location, int numRooms, int price)
            throws RemoteException;

    public boolean deleteRooms(int id, String location)
            throws RemoteException;

    public int queryRooms(int id, String location)
            throws RemoteException;

    public int queryRoomsPrice(int id, String location)
            throws RemoteException;

    public boolean reserveRoom(int id, int customerID, String location)
            throws RemoteException;

    public String getName()
            throws RemoteException;
}
