package Constants;

/**
 * Created by alex on 9/25/18.
 */
public class GeneralConstants {
    public static final String groupPrefix = "group01";

    public static final String CHAR_SET = "UTF-8";

    public static final String ACTION = "action";
    public static final String RESULT = "result";
    public static final String TYPE = "type";

    //TYPES
    public static final String ROOM_ENTITY = "rooms";
    public static final String FLIGHT_ENTITY = "flights";
    public static final String CAR_ENTITY = "cars";
    public static final String CUSTOMER_ENTITY = "customer";
    public static final String TRANSACTION = "transaction";
    public static final String OTHERS = "others";

    //CAR
    public static final String ADD_CARS = "AddCars";
    public static final String DELETE_CARS = "DeleteCars";
    public static final String QUERY_CARS = "QueryCars";
    public static final String QUERY_CARS_PRICE = "QueryCarsPrice";
    public static final String RESERVE_CARS = "ReserveCars";

    public static final String CAR_LOCATION = "location";
    public static final String CAR_COUNT = "count";
    public static final String CAR_PRICE = "price";
    public static final String CAR_CUSTOMER_ID = "customerId";

    //FLIGHT
    public static final String ADD_FLIGHTS = "AddFlight";
    public static final String DELETE_FLIGHTS = "DeleteFlight";
    public static final String QUERY_FLIGHTS = "QueryFlight";
    public static final String QUERY_FLIGHTS_PRICE = "QueryFlightPrice";
    public static final String RESERVE_FLIGHT = "ReserveFlight";
    public static final String FLIGHT_NUMBER = "flightNumber";
    public static final String FLIGHT_SEATS = "seats";
    public static final String FLIGHT_PRICE = "price";


    //ROOMS
    public static final String ADD_ROOMS = "AddRooms";
    public static final String DELETE_ROOMS = "DeleteRooms";
    public static final String QUERY_ROOMS = "QueryRooms";
    public static final String QUERY_ROOMS_PRICE = "QueryRoomsPrice";
    public static final String RESERVE_ROOMS = "ReserveRooms";

    public static final String ROOM_LOCATION = "location";
    public static final String ROOM_COUNT = "count";
    public static final String ROOM_PRICE = "price";

    //CUSTOMER
    public static final String CUSTOMER_ID = "customerId";

    public static final String NEW_CUSTOMER = "AddCustomer";
    public static final String NEW_CUSTOMER_ID = "AddCustomerID";
    public static final String DELETE_CUSTOMER = "DeleteCustomer";
    public static final String QUERY_CUSTOMER = "QueryCustomer";

    //BUNDLE
    public static final String BUNDLE = "Bundle";
    public static final String FLIGHT_NUMBERS = "flightNumbers";
    public static final String BOOK_CAR = "bookCar";
    public static final String BOOK_ROOM = "bookFlight";

    //TRANSACTION
    public static final String XID = "xid";
    public static final String NEW_TRANSACTION = "NewTransaction";
    public static final String COMMIT = "Commit";
    public static final String DEADLOCK = "Deadlock";
    public static final String ABORTED = "Aborted";
    public static final String ABORT = "Abort";
    public static final String VALID_XID = "ValidXid";

    //SHUTDOWN
    public static final String SHUTDOWN = "Shutdown";

    // DELAY
    public static final int TIMER_DELAY = 10000;

}
