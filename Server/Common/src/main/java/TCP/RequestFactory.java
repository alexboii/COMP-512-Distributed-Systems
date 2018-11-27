package TCP;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Vector;

import static Constants.GeneralConstants.*;
import static Constants.GeneralConstants.XID;
import static Constants.GeneralConstants.FLIGHT_NUMBER;

/**
 * Created by alex on 10/2/18.
 */
public class RequestFactory {
    public static JSONObject getAddFlightRequest(int id, int flightNum, int flightSeats, int flightPrice) throws JSONException {

        JSONObject request = new JSONObject();

        request.put(TYPE, FLIGHT_ENTITY);
        request.put(ACTION, ADD_FLIGHTS);
        request.put(XID, id);
        request.put(FLIGHT_NUMBER, flightNum);
        request.put(FLIGHT_SEATS, flightSeats);
        request.put(FLIGHT_PRICE, flightPrice);

        return request;

    }

    public static JSONObject getAddCarRequest(int xid, String location, int count, int price) throws JSONException {

        JSONObject request = new JSONObject();

        request.put(TYPE, CAR_ENTITY);
        request.put(ACTION, ADD_CARS);
        request.put(XID, xid);
        request.put(CAR_LOCATION, location);
        request.put(CAR_COUNT, count);
        request.put(CAR_PRICE, price);

        return request;
    }

    public static JSONObject getAddRoomRequest(int xid, String location, int count, int price) throws JSONException {

        JSONObject request = new JSONObject();

        request.put(TYPE, ROOM_ENTITY);
        request.put(ACTION, ADD_ROOMS);
        request.put(XID, xid);
        request.put(ROOM_LOCATION, location);
        request.put(ROOM_COUNT, count);
        request.put(ROOM_PRICE, price);

        return request;
    }

    public static JSONObject getAddCustomerRequest(int xid) throws JSONException {
        JSONObject request = new JSONObject();

        request.put(TYPE, CUSTOMER_ENTITY);
        request.put(ACTION, NEW_CUSTOMER);
        request.put(XID, xid);

        return request;
    }

    public static JSONObject getAddCustomerIdRequest(int xid, int cid) throws JSONException {
        JSONObject request = new JSONObject();

        request.put(TYPE, CUSTOMER_ENTITY);
        request.put(ACTION, NEW_CUSTOMER_ID);
        request.put(XID, xid);
        request.put(CUSTOMER_ID, cid);

        return request;
    }

    public static JSONObject getDeleteFlightRequest(int xid, int flightNum) throws JSONException {
        JSONObject request = new JSONObject();

        request.put(TYPE, FLIGHT_ENTITY);
        request.put(ACTION, DELETE_FLIGHTS);
        request.put(XID, xid);
        request.put(FLIGHT_NUMBER, flightNum);

        return request;
    }

    public static JSONObject getDeleteCarRequest(int xid, String location) throws JSONException {
        JSONObject request = new JSONObject();

        request.put(TYPE, CAR_ENTITY);
        request.put(ACTION, DELETE_CARS);
        request.put(XID, xid);
        request.put(CAR_LOCATION, location);

        return request;
    }

    public static JSONObject getDeleteRoomRequest(int xid, String location) throws JSONException {
        JSONObject request = new JSONObject();

        request.put(TYPE, ROOM_ENTITY);
        request.put(ACTION, DELETE_ROOMS);
        request.put(XID, xid);
        request.put(ROOM_LOCATION, location);

        return request;
    }

    public static JSONObject getDeleteCustomerRequest(int xid, int cid) throws JSONException {
        JSONObject request = new JSONObject();

        request.put(TYPE, CUSTOMER_ENTITY);
        request.put(ACTION, DELETE_CUSTOMER);
        request.put(XID, xid);
        request.put(CUSTOMER_ID, cid);

        return request;
    }

    public static JSONObject getQueryFlightRequest(int xid, int flightNum) throws JSONException {
        JSONObject request = new JSONObject();

        request.put(TYPE, FLIGHT_ENTITY);
        request.put(ACTION, QUERY_FLIGHTS);
        request.put(XID, xid);
        request.put(FLIGHT_NUMBER, flightNum);

        return request;
    }

    public static JSONObject getQueryRoomRequest(int xid, String location) throws JSONException {
        JSONObject request = new JSONObject();

        request.put(TYPE, ROOM_ENTITY);
        request.put(ACTION, QUERY_ROOMS);
        request.put(XID, xid);
        request.put(ROOM_LOCATION, location);

        return request;
    }

    public static JSONObject getQueryCarRequest(int xid, String location) throws JSONException {
        JSONObject request = new JSONObject();

        request.put(TYPE, CAR_ENTITY);
        request.put(ACTION, QUERY_CARS);
        request.put(XID, xid);
        request.put(CAR_LOCATION, location);

        return request;
    }

    public static JSONObject getQueryCustomerRequest(int xid, int cid) throws JSONException {
        JSONObject request = new JSONObject();

        request.put(TYPE, CUSTOMER_ENTITY);
        request.put(ACTION, QUERY_CUSTOMER);
        request.put(XID, xid);
        request.put(CUSTOMER_ID, cid);

        return request;
    }

    public static JSONObject getQueryFlightPriceRequest(int xid, int flightNum) throws JSONException {
        JSONObject request = new JSONObject();

        request.put(TYPE, FLIGHT_ENTITY);
        request.put(ACTION, QUERY_FLIGHTS_PRICE);
        request.put(XID, xid);
        request.put(FLIGHT_NUMBER, flightNum);

        return request;
    }

    public static JSONObject getQueryRoomPriceRequest(int xid, String location) throws JSONException {
        JSONObject request = new JSONObject();

        request.put(TYPE, ROOM_ENTITY);
        request.put(ACTION, QUERY_ROOMS_PRICE);
        request.put(XID, xid);
        request.put(ROOM_LOCATION, location);

        return request;
    }

    public static JSONObject getQueryCarPriceRequest(int xid, String location) throws JSONException {
        JSONObject request = new JSONObject();

        request.put(TYPE, CAR_ENTITY);
        request.put(ACTION, QUERY_CARS_PRICE);
        request.put(XID, xid);
        request.put(CAR_LOCATION, location);

        return request;
    }

    public static JSONObject getReserveFlightRequest(int xid, int cid, int flightNum) throws JSONException {
        JSONObject request = new JSONObject();

        request.put(TYPE, CUSTOMER_ENTITY);
        request.put(ACTION, RESERVE_FLIGHT);
        request.put(XID, xid);
        request.put(CUSTOMER_ID, cid);
        request.put(FLIGHT_NUMBER, flightNum);

        return request;
    }

    public static JSONObject getReserveCarRequest(int xid, int cid, String location) throws JSONException {
        JSONObject request = new JSONObject();

        request.put(TYPE, CUSTOMER_ENTITY);
        request.put(ACTION, RESERVE_CARS);
        request.put(XID, xid);
        request.put(CUSTOMER_ID, cid);
        request.put(CAR_LOCATION, location);

        return request;
    }

    public static JSONObject getReserveRoomRequest(int xid, int cid, String location) throws JSONException {
        JSONObject request = new JSONObject();

        request.put(TYPE, CUSTOMER_ENTITY);
        request.put(ACTION, RESERVE_ROOMS);
        request.put(XID, xid);
        request.put(CUSTOMER_ID, cid);
        request.put(ROOM_LOCATION, location);

        return request;
    }


    public static JSONObject getBundleRequest(int xid, int cid, Vector<String> flightNumbers, String location,
                                              boolean bookCar, boolean bookRoom) throws JSONException {
        JSONObject request = new JSONObject();

        request.put(TYPE, CUSTOMER_ENTITY);
        request.put(ACTION, BUNDLE);
        request.put(XID, xid);
        request.put(CUSTOMER_ID, cid);
        request.put(FLIGHT_NUMBERS, new JSONArray(flightNumbers));
        request.put(ROOM_LOCATION, location);
        request.put(BOOK_CAR, bookCar);
        request.put(BOOK_ROOM, bookRoom);

        return request;
    }

    public static JSONObject getNewTransactionRequest() throws JSONException {
        JSONObject request = new JSONObject();

        request.put(TYPE, TRANSACTION);
        request.put(ACTION, NEW_TRANSACTION);
        return request;
    }

    public static JSONObject getCommitRequest(int xid) throws JSONException {
        JSONObject request = new JSONObject();

        request.put(TYPE, TRANSACTION);
        request.put(ACTION, COMMIT);
        request.put(XID, xid);
        return request;
    }

    public static JSONObject getAbortRequest(int xid) throws JSONException {
        JSONObject request = new JSONObject();

        request.put(TYPE, TRANSACTION);
        request.put(ACTION, ABORT);
        request.put(XID, xid);
        return request;
    }

    public static JSONObject getShutdownRequest() throws JSONException {
        JSONObject request = new JSONObject();

        request.put(TYPE, OTHERS);
        request.put(ACTION, SHUTDOWN);
        return request;
    }

    public static JSONObject getCrashMiddlewareRequest(int mode) throws JSONException {
        JSONObject request = new JSONObject();

        request.put(TYPE, CRASH);
        request.put(ACTION, CRASH_MIDDLEWARE);
        request.put(CRASH_MODE, mode);
        return request;
    }

    public static JSONObject getCrashResourceManagerRequest(String resourceManager, int mode) throws JSONException {
        JSONObject request = new JSONObject();

        request.put(TYPE, CRASH);
        request.put(ACTION, CRASH_RESOURCE_MANAGER);
        request.put(RESOURCE_MANAGER_NAME, resourceManager);
        request.put(CRASH_MODE, mode);
        return request;
    }

    public static JSONObject getResetCrashesRequest() throws JSONException {
        JSONObject request = new JSONObject();

        request.put(TYPE, CRASH);
        request.put(ACTION, RESET_CRASHES);
        return request;
    }

    public static JSONObject getVoteRequest(int xid) throws JSONException {
        JSONObject request = new JSONObject();

        request.put(TYPE, TRANSACTION);
        request.put(ACTION, VOTE_REQUEST);
        request.put(XID, xid);

        return request;
    }

    public static JSONObject getDecisionRequest(int xid, boolean decision) throws JSONException {
        JSONObject request = new JSONObject();

        request.put(TYPE, TRANSACTION);
        request.put(ACTION, DECISION);
        request.put(XID, xid);
        request.put(DECISION_FIELD, decision);

        return request;
    }

    public static JSONObject getGetDecisionRequest(int xid) throws JSONException {
        JSONObject request = new JSONObject();

        request.put(TYPE, TRANSACTION);
        request.put(ACTION, GET_DECISION);
        request.put(XID, xid);

        return request;
    }

    public static JSONObject getHaveCommittedRequest(int xid, String rmAddress) throws JSONException {
        JSONObject request = new JSONObject();

        request.put(TYPE, TRANSACTION);
        request.put(ACTION, HAVE_COMMITTED);
        request.put(RM_ADDRESS, rmAddress);
        request.put(XID, xid);

        return request;
    }

}

