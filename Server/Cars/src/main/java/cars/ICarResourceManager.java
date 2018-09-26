package cars;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * Created by alex on 9/25/18.
 */
public interface ICarResourceManager extends Remote {
    public boolean addCars(int id, String location, int numCars, int price)
            throws RemoteException;

    public boolean deleteCars(int id, String location)
            throws RemoteException;

    public int queryCars(int id, String location)
            throws RemoteException;

    public int queryCarsPrice(int id, String location)
            throws RemoteException;

    public boolean reserveCar(int id, int customerID, String location)
            throws RemoteException;

    public String getName()
            throws RemoteException;
}
