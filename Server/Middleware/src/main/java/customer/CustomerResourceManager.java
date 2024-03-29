package customer;

import LockManager.DeadlockException;
import Model.*;
import RM.ResourceManager;
import Utilities.Trace;
import org.json.JSONObject;

import java.io.OutputStreamWriter;
import java.util.Calendar;

/**
 * Created by alex on 10/1/18.
 */
public class CustomerResourceManager extends ResourceManager {
    private static final String serverName = "Customers";

    public CustomerResourceManager() {
        super(serverName);
    }

    // Reserve an item
    protected boolean reserveItem(int xid, int customerID, String key, String location, int price) throws DeadlockException {
        Customer customer = (Customer) transactionManager.readDataTransaction(xid, Customer.getKey(customerID));
        if (customer == null) {
            Trace.warn("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ")  failed--customer doesn't exist");
            return false;
        }
        // since this method will only be called once
        customer.reserve(key, location, price);
        transactionManager.writeDataTransaction(xid, customer.getKey(), customer);

        Trace.info("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ") succeeded");
        return true;
    }

    @Override
    public boolean deleteCustomer(int xid, int customerID) throws DeadlockException {
        Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") called");
        Customer customer = (Customer) transactionManager.readDataTransaction(xid, Customer.getKey(customerID));
        if (customer == null) {
            Trace.warn("RM::deleteCustomer(" + xid + ", " + customerID + ") failed--customer doesn't exist");
            return false;
        } else {
            // Remove the customer from the storage
            transactionManager.removeDataTransaction(xid, customer.getKey());
            Trace.info("RM::deleteCustomer(" + xid + ", " + customerID + ") succeeded");
            return true;
        }
    }

    // Adds flight reservation to this customer
    public boolean reserveFlight(int xid, int customerID, int flightNum, int price) throws DeadlockException {
        return reserveItem(xid, customerID, Flight.getKey(flightNum), String.valueOf(flightNum), price);
    }

    // Adds car reservation to this customer
    public boolean reserveCar(int xid, int customerID, String location, int price) throws DeadlockException {
        return reserveItem(xid, customerID, Car.getKey(location), location, price);
    }

    // Adds room reservation to this customer
    public boolean reserveRoom(int xid, int customerID, String location, int price) throws DeadlockException {
        return reserveItem(xid, customerID, Room.getKey(location), location, price);
    }

    @Override
    public void handleRequest(JSONObject request, OutputStreamWriter writer) {
        return;
    }

    public int generateCID(int xid) {
        return Integer.parseInt(String.valueOf(xid) +
                String.valueOf(Calendar.getInstance().get(Calendar.MILLISECOND)) +
                String.valueOf(Math.round(Math.random() * 100 + 1)));
    }
}




