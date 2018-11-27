package RM;

import Model.*;
import TM.TransactionManager;
import Utilities.FileLogger;
import LockManager.*;

/**
 * Created by alex on 9/25/18.
 */
// -------------------------------
// adapted from Kevin T. Manley
// CSE 593
// -------------------------------

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

abstract public class ResourceManager implements IResourceManager {
    protected String m_name = "";
    protected TransactionManager transactionManager;
    protected AtomicInteger rMCrashMode = new AtomicInteger(0);

    private static final Logger logger = FileLogger.getLogger(ResourceManager.class);

    public ResourceManager(String p_name) {
        m_name = p_name;
        this.transactionManager = new TransactionManager(m_name);
    }

    // Query the number of available seats/rooms/cars
    protected int queryNum(int xid, String key) throws DeadlockException {
        logger.info("RM::queryNum(" + xid + ", " + key + ") called");
        ReservableItem curObj = (ReservableItem) transactionManager.readDataTransaction(xid, key);
        int value = 0;
        if (curObj != null) {
            value = curObj.getCount();
        }
        logger.info("RM::queryNum(" + xid + ", " + key + ") returns count=" + value);
        return value;
    }

    protected int queryPrice(int xid, String key) throws DeadlockException {
        logger.info("RM::queryPrice(" + xid + ", " + key + ") called");
        ReservableItem curObj = (ReservableItem) transactionManager.readDataTransaction(xid, key);
        int value = 0;
        if (curObj != null) {
            value = curObj.getPrice();
        }
        logger.info("RM::queryPrice(" + xid + ", " + key + ") returns cost=$" + value);
        return value;
    }

    protected boolean reserveItem(int xid, int customerID, String key, String location) throws DeadlockException {
        logger.info("RM::reserveItem(" + xid + ", customer=" + customerID + ", " + key + ", " + location + ") called");
        // Read customer object if it exists (and read lock it)
        Customer customer = (Customer) transactionManager.readDataTransaction(xid, Customer.getKey(customerID));
        if (customer == null) {
            logger.warning("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ")  failed--customer doesn't exist");
            return false;
        }

        // Check if the item is available
        ReservableItem item = (ReservableItem) transactionManager.readDataTransaction(xid, key);
        if (item == null) {
            logger.warning("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ") failed--item doesn't exist");
            return false;
        } else if (item.getCount() == 0) {
            logger.warning("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ") failed--No more items");
            return false;
        } else {
            customer.reserve(key, location, item.getPrice());
            transactionManager.writeDataTransaction(xid, customer.getKey(), customer);

            // Decrease the number of available items in the storage
            item.setCount(item.getCount() - 1);
            item.setReserved(item.getReserved() + 1);
            transactionManager.writeDataTransaction(xid, item.getKey(), item);

            logger.info("RM::reserveItem(" + xid + ", " + customerID + ", " + key + ", " + location + ") succeeded");
            return true;
        }
    }

    public boolean deleteItemTransaction(int xid, String key) throws DeadlockException {
        logger.info("RM::deleteItem(" + xid + ", " + key + ") called");
        ReservableItem curObj = (ReservableItem) transactionManager.readDataTransaction(xid, key);
        // Check if there is such an item in the storage
        if (curObj == null) {
            logger.warning("RM::deleteItem(" + xid + ", " + key + ") failed--item doesn't exist");
            return false;
        } else {
            if (curObj.getReserved() == 0) {
                transactionManager.removeDataTransaction(xid, curObj.getKey());
                logger.info("RM::deleteItem(" + xid + ", " + key + ") item deleted");
                return true;
            } else {
                logger.info("RM::deleteItem(" + xid + ", " + key + ") item can't be deleted because some customers have reserved it");
                return false;
            }
        }
    }

    // Create a new flight, or add seats to existing flight
    // NOTE: if flightPrice <= 0 and the flight already exists, it maintains its current price
    public boolean addFlight(int xid, int flightNum, int flightSeats, int flightPrice) throws DeadlockException {
        logger.info("RM::addFlight(" + xid + ", " + flightNum + ", " + flightSeats + ", $" + flightPrice + ") called");
        Flight curObj = (Flight) transactionManager.readDataTransaction(xid, Flight.getKey(flightNum));
        if (curObj == null) {
            // Doesn't exist yet, add it
            Flight newObj = new Flight(flightNum, flightSeats, flightPrice);
            transactionManager.writeDataTransaction(xid, newObj.getKey(), newObj);
            logger.info("RM::addFlight(" + xid + ") created new flight " + flightNum + ", seats=" + flightSeats + ", price=$" + flightPrice);
        } else {
            // Add seats to existing flight and update the price if greater than zero
            curObj.setCount(curObj.getCount() + flightSeats);
            if (flightPrice > 0) {
                curObj.setPrice(flightPrice);
            }
            transactionManager.writeDataTransaction(xid, curObj.getKey(), curObj);
            logger.info("RM::addFlight(" + xid + ") modified existing flight " + flightNum + ", seats=" + curObj.getCount() + ", price=$" + flightPrice);
        }
        return true;
    }

    // Create a new car location or add cars to an existing location
    // NOTE: if price <= 0 and the location already exists, it maintains its current price
    public boolean addCars(int xid, String location, int count, int price) throws DeadlockException {
        logger.info("RM::addCars(" + xid + ", " + location + ", " + count + ", $" + price + ") called");

        Car carObj = (Car) transactionManager.readDataTransaction(xid, Car.getKey(location));

        if (carObj == null) {
            // Car location doesn't exist yet, add it
            Car newObj = new Car(location, count, price);

            transactionManager.writeDataTransaction(xid, newObj.getKey(), newObj);
            logger.info("RM::addCars(" + xid + ") created new location " + location + ", count=" + count + ", price=$" + price);
        } else {
            // Add count to existing car location and update price if greater than zero
            carObj.setCount(carObj.getCount() + count);
            if (price > 0) {
                carObj.setPrice(price);
            }
            transactionManager.writeDataTransaction(xid, carObj.getKey(), carObj);
            logger.info("RM::addCars(" + xid + ") modified existing location " + location + ", count=" + carObj.getCount() + ", price=$" + price);
        }
        return true;
    }

    // Create a new room location or add rooms to an existing location
    // NOTE: if price <= 0 and the room location already exists, it maintains its current price
    public boolean addRooms(int xid, String location, int count, int price) throws DeadlockException {
        logger.info("RM::addRooms(" + xid + ", " + location + ", " + count + ", $" + price + ") called");
        Room curObj = (Room) transactionManager.readDataTransaction(xid, Room.getKey(location));
        if (curObj == null) {
            // Room location doesn't exist yet, add it
            Room newObj = new Room(location, count, price);
            transactionManager.writeDataTransaction(xid, newObj.getKey(), newObj);
            logger.info("RM::addRooms(" + xid + ") created new room location " + location + ", count=" + count + ", price=$" + price);
        } else {
            // Add count to existing object and update price if greater than zero
            curObj.setCount(curObj.getCount() + count);
            if (price > 0) {
                curObj.setPrice(price);
            }
            transactionManager.writeDataTransaction(xid, curObj.getKey(), curObj);
            logger.info("RM::addRooms(" + xid + ") modified existing location " + location + ", count=" + curObj.getCount() + ", price=$" + price);
        }
        return true;
    }

    // Deletes flight
    public boolean deleteFlight(int xid, int flightNum) throws DeadlockException {
        return deleteItemTransaction(xid, Flight.getKey(flightNum));
    }

    // Delete cars at a location
    public boolean deleteCars(int xid, String location) throws DeadlockException {
        return deleteItemTransaction(xid, Car.getKey(location));
    }

    // Delete rooms at a location
    public boolean deleteRooms(int xid, String location) throws DeadlockException {
        return deleteItemTransaction(xid, Room.getKey(location));
    }

    // Returns the number of empty seats in this flight
    public int queryFlight(int xid, int flightNum) throws DeadlockException {
        return queryNum(xid, Flight.getKey(flightNum));
    }

    // Returns the number of cars available at a location
    public int queryCars(int xid, String location) throws DeadlockException {
        return queryNum(xid, Car.getKey(location));
    }

    // Returns the amount of rooms available at a location
    public int queryRooms(int xid, String location) throws DeadlockException {
        return queryNum(xid, Room.getKey(location));
    }

    // Returns price of a seat in this flight
    public int queryFlightPrice(int xid, int flightNum) throws DeadlockException {
        return queryPrice(xid, Flight.getKey(flightNum));
    }

    // Returns price of cars at this location
    public int queryCarsPrice(int xid, String location) throws DeadlockException {
        return queryPrice(xid, Car.getKey(location));
    }

    // Returns room price at this location
    public int queryRoomsPrice(int xid, String location) throws DeadlockException {
        return queryPrice(xid, Room.getKey(location));
    }

    public String queryCustomerInfo(int xid, int customerID) throws DeadlockException {
        logger.info("RM::queryCustomerInfo(" + xid + ", " + customerID + ") called");
        Customer customer = (Customer) transactionManager.readDataTransaction(xid, Customer.getKey(customerID));
        if (customer == null) {
            logger.warning("RM::queryCustomerInfo(" + xid + ", " + customerID + ") failed--customer doesn't exist");
            // NOTE: don't change this--WC counts on this value indicating a customer does not exist...
            return "";
        } else {
            logger.info("RM::queryCustomerInfo(" + xid + ", " + customerID + ")");
            logger.info(customer.getBill());
            return customer.getBill();
        }
    }

    public boolean newCustomer(int xid, int customerID) throws DeadlockException {
        logger.info("RM::newCustomer(" + xid + ", " + customerID + ") called");
        Customer customer = (Customer) transactionManager.readDataTransaction(xid, Customer.getKey(customerID));
        if (customer == null) {
            customer = new Customer(customerID);
            transactionManager.writeDataTransaction(xid, customer.getKey(), customer);
            logger.info("RM::newCustomer(" + xid + ", " + customerID + ") created a new customer");
            return true;
        } else {
            logger.info("INFO: RM::newCustomer(" + xid + ", " + customerID + ") failed--customer already exists");
            return false;
        }
    }

    public boolean deleteCustomer(int xid, int customerID) throws DeadlockException {
        logger.info("RM::deleteCustomer(" + xid + ", " + customerID + ") called");
        Customer customer = (Customer) transactionManager.readDataTransaction(xid, Customer.getKey(customerID));
        if (customer == null) {
            logger.warning("RM::deleteCustomer(" + xid + ", " + customerID + ") failed--customer doesn't exist");
            return false;
        } else {
            // Increase the reserved numbers of all reservable items which the customer reserved.
            RMHashMap reservations = customer.getReservations();
            for (String reservedKey : reservations.keySet()) {
                ReservedItem reserveditem = customer.getReservedItem(reservedKey);
                logger.info("RM::deleteCustomer(" + xid + ", " + customerID + ") has reserved " + reserveditem.getKey() + " " + reserveditem.getCount() + " times");
                ReservableItem item = (ReservableItem) transactionManager.readDataTransaction(xid, reserveditem.getKey());
                logger.info("RM::deleteCustomer(" + xid + ", " + customerID + ") has reserved " + reserveditem.getKey() + " which is reserved " + item.getReserved() + " times and is still available " + item.getCount() + " times");
                item.setReserved(item.getReserved() - reserveditem.getCount());
                item.setCount(item.getCount() + reserveditem.getCount());
                transactionManager.writeDataTransaction(xid, item.getKey(), item);
            }

            // Remove the customer from the storage
            transactionManager.removeDataTransaction(xid, customer.getKey());
            logger.info("RM::deleteCustomer(" + xid + ", " + customerID + ") succeeded");
            return true;
        }
    }

    // Adds flight reservation to this customer
    public boolean reserveFlight(int xid, int customerID, int flightNum) throws DeadlockException {
        return reserveItem(xid, customerID, Flight.getKey(flightNum), String.valueOf(flightNum));
    }

    // Adds car reservation to this customer
    public boolean reserveCar(int xid, int customerID, String location) throws DeadlockException {
        return reserveItem(xid, customerID, Car.getKey(location), location);
    }

    // Adds room reservation to this customer
    public boolean reserveRoom(int xid, int customerID, String location) throws DeadlockException {
        return reserveItem(xid, customerID, Room.getKey(location), location);
    }

    // Reserve bundle
    public boolean bundle(int xid, int customerId, Vector<String> flightNumbers, String location, boolean car, boolean room) {
        return false;
    }

    public boolean voteReply(int xid) {

        if (rMCrashMode.get() == 1){
            //Crash after receive vote request but before sending answer
            logger.info("Simulating Resource Manager crash mode=" + rMCrashMode);
            System.exit(1);
        }

        boolean reply = transactionManager.voteReply(xid);

        if (rMCrashMode.get() == 2){
            //Crash after deciding which answer to send (commit/abort)
            logger.info("Simulating Resource Manager crash mode=" + rMCrashMode);
            System.exit(1);
        }

        return reply;
    }

    public String getName() {
        return m_name;
    }

    public boolean commit(int id) {
        return transactionManager.commit(id);
    }

    public boolean abort(int id) {
        return transactionManager.abort(id);
    }

    protected void setRMCrashMode(int mode){
        logger.info("Enabling Resource Manager crash mode=" + mode);
        rMCrashMode.set(mode);
    }

    protected void resetCrash() {
        logger.info("Resetting crash modes");
        rMCrashMode.set(0);
    }
}

