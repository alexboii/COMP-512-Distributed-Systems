package middleware;

import Constants.ServerConstants;
import RM.ResourceManager;
import Tcp.IServer;
import Tcp.RequestFactory;
import Tcp.SocketUtils;
import Utilities.FileLogger;
import customer.CustomerResourceManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import transaction.XIDManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.logging.Logger;

import static Constants.GeneralConstants.*;
import static Tcp.SocketUtils.sendReply;

public class Middleware extends ResourceManager implements IServer {
    private static final String serverName = "Middleware";
    private static final int maxConcurrentClients = 10;
    public CustomerResourceManager customerManager;
    private XIDManager xIDManager;

    private static final Logger logger = FileLogger.getLogger(Middleware.class);

    public static void main(String[] args) {
        Middleware obj = new Middleware();
        obj.start(ServerConstants.MIDDLEWARE_PORT);
    }

    public Middleware() {
        super(serverName);
        customerManager = new CustomerResourceManager();
        xIDManager = new XIDManager();
    }


    @Override
    public void handleRequest(JSONObject request, OutputStreamWriter writer) throws IOException, JSONException {
        switch ((String) request.get(TYPE)) {
            // redirection happens here
            case CAR_ENTITY:
                JSONObject res = sendAndReceiveAgnostic(ServerConstants.CAR_SERVER_ADDRESS, ServerConstants.CAR_SERVER_PORT, request);
                sendReply(writer, res);
                break;
            case FLIGHT_ENTITY:
                res = sendAndReceiveAgnostic(ServerConstants.FLIGHTS_SERVER_ADDRESS, ServerConstants.FLIGHTS_SERVER_PORT, request);
                sendReply(writer, res);
                break;
            case ROOM_ENTITY:
                res = sendAndReceiveAgnostic(ServerConstants.ROOMS_SERVER_ADDRESS, ServerConstants.ROOMS_SERVER_PORT, request);
                sendReply(writer, res);
                break;
            case CUSTOMER_ENTITY:
                switch ((String) request.get(ACTION)) {
                    case NEW_CUSTOMER:
                        int result = newCustomer(request);
                        sendReply(writer, result);
                        break;
                    case NEW_CUSTOMER_ID:
                        boolean boolRes = newCustomerId(request);
                        sendReply(writer, boolRes);
                        break;
                    case DELETE_CUSTOMER:
                        boolRes = deleteCustomer(request);
                        sendReply(writer, boolRes);
                        break;
                    case QUERY_CUSTOMER:
                        String strRes = queryCustomerInfo(request);
                        sendReply(writer, strRes);
                        break;
                    case RESERVE_CARS:
                        boolRes = reserveCar(request);
                        sendReply(writer, boolRes);
                        break;
                    case RESERVE_FLIGHT:
                        boolRes = reserveFlight(request);
                        sendReply(writer, boolRes);
                        break;
                    case RESERVE_ROOMS:
                        boolRes = reserveRoom(request);
                        sendReply(writer, boolRes);
                        break;
                    case BUNDLE:
                        boolRes = bundle(request);
                        sendReply(writer, boolRes);
                        break;
                }
                break;

            case TRANSACTION:

                switch ((String) request.get(ACTION)) {

                    case NEW_TRANSACTION:
                        int result = xIDManager.newTransaction();
                        sendReply(writer, result);
                        break;

                    case COMMIT:
                        boolean boolRes = sendRequestToAllServers(request);
                        sendReply(writer, boolRes);
                        break;

                    case ABORT:
                        boolRes = sendRequestToAllServers(request);
                        sendReply(writer, boolRes);
                        break;
                }
                break;
        }
    }

    /**
     * Routing happens here
     *
     * @param serverAddress
     * @param port
     * @param request
     * @return
     */
    private JSONObject sendAndReceiveAgnostic(String serverAddress, int port, JSONObject request) throws JSONException {
        JSONObject result = null;
        boolean abort = false;

        try {
            logger.info("Sending request " + request + "to server: " + serverAddress + ":" + port);
            Socket server = new Socket(InetAddress.getByName(serverAddress), port);
            OutputStreamWriter writer = new OutputStreamWriter(server.getOutputStream(), CHAR_SET);
            BufferedReader reader = new BufferedReader(new InputStreamReader(server.getInputStream(), CHAR_SET));

            logger.info("Successfully sent request " + request + "to server: " + serverAddress + ":" + port);

            result = SocketUtils.sendAndReceive(request, writer, reader);

            if(result.has(SHOULD_ABORT)) {
                abort = result.getBoolean(SHOULD_ABORT);
            }

            server.close();
            writer.close();
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(abort){
            logger.info("Aborting transaction: " + request.getInt(XID));
            JSONObject abortRequest = RequestFactory.getAbortRequest(request.getInt(XID));
            sendRequestToAllServers(abortRequest);
        }

        return result;
    }

    private boolean sendRequestToAllServers(JSONObject request) throws JSONException {
        logger.info("Sending request to all servers. " + request);
        return sendAndReceiveAgnostic(ServerConstants.CAR_SERVER_ADDRESS, ServerConstants.CAR_SERVER_PORT, request).getBoolean(RESULT) &&
                sendAndReceiveAgnostic(ServerConstants.FLIGHTS_SERVER_ADDRESS, ServerConstants.FLIGHTS_SERVER_PORT, request).getBoolean(RESULT) &&
                sendAndReceiveAgnostic(ServerConstants.ROOMS_SERVER_ADDRESS, ServerConstants.ROOMS_SERVER_PORT, request).getBoolean(RESULT);
    }


    public int newCustomer(JSONObject request) throws JSONException {
        int xid = request.getInt(XID);

        JSONObject replyCar = sendAndReceiveAgnostic(ServerConstants.CAR_SERVER_ADDRESS, ServerConstants.CAR_SERVER_PORT, request);

        if (replyCar == null) {
            return 0;
        }

        JSONObject replyFlights = sendAndReceiveAgnostic(ServerConstants.FLIGHTS_SERVER_ADDRESS, ServerConstants.FLIGHTS_SERVER_PORT, request);

        if (replyFlights == null) {
            return 0;
        }

        JSONObject replyRooms = sendAndReceiveAgnostic(ServerConstants.ROOMS_SERVER_ADDRESS, ServerConstants.ROOMS_SERVER_PORT, request);

        if (replyRooms == null) {
            return 0;
        }


        return customerManager.newCustomer(xid) > 0 && replyFlights.getInt(RESULT) > 0 && replyCar.getInt(RESULT) > 0 && replyRooms.getInt(RESULT) > 0 ? 1 : 0;
    }

    public boolean newCustomerId(JSONObject request) throws JSONException {
        int xid = request.getInt(XID);
        int cid = request.getInt(CUSTOMER_ID);

        JSONObject replyCar = sendAndReceiveAgnostic(ServerConstants.CAR_SERVER_ADDRESS, ServerConstants.CAR_SERVER_PORT, request);

        if (replyCar == null) {
            return false;
        }

        JSONObject replyFlights = sendAndReceiveAgnostic(ServerConstants.FLIGHTS_SERVER_ADDRESS, ServerConstants.FLIGHTS_SERVER_PORT, request);

        if (replyFlights == null) {
            return false;
        }

        JSONObject replyRooms = sendAndReceiveAgnostic(ServerConstants.ROOMS_SERVER_ADDRESS, ServerConstants.ROOMS_SERVER_PORT, request);

        if (replyRooms == null) {
            return false;
        }

        return customerManager.newCustomer(xid, cid) && replyCar.getBoolean(RESULT) && replyFlights.getBoolean(RESULT) && replyRooms.getBoolean(RESULT);
    }

    public boolean deleteCustomer(JSONObject request) throws JSONException {
        int xid = request.getInt(XID);
        int cid = request.getInt(CUSTOMER_ID);

        JSONObject replyCar = sendAndReceiveAgnostic(ServerConstants.CAR_SERVER_ADDRESS, ServerConstants.CAR_SERVER_PORT, request);

        if (replyCar == null) {
            return false;
        }

        JSONObject replyFlights = sendAndReceiveAgnostic(ServerConstants.FLIGHTS_SERVER_ADDRESS, ServerConstants.FLIGHTS_SERVER_PORT, request);

        if (replyFlights == null) {
            return false;
        }

        JSONObject replyRooms = sendAndReceiveAgnostic(ServerConstants.ROOMS_SERVER_ADDRESS, ServerConstants.ROOMS_SERVER_PORT, request);

        if (replyRooms == null) {
            return false;
        }

        return customerManager.deleteCustomer(xid, cid) && replyCar.getBoolean(RESULT) && replyFlights.getBoolean(RESULT) && replyRooms.getBoolean(RESULT);
    }


    public String queryCustomerInfo(JSONObject request) throws JSONException {
        int xid = request.getInt(XID);
        int cid = request.getInt(CUSTOMER_ID);

        return customerManager.queryCustomerInfo(xid, cid);
    }


    public boolean reserveFlight(JSONObject request) throws JSONException {
        JSONObject replyFlights = sendAndReceiveAgnostic(ServerConstants.FLIGHTS_SERVER_ADDRESS, ServerConstants.FLIGHTS_SERVER_PORT, request);

        if (replyFlights == null || !replyFlights.getBoolean(RESULT)) {
            return false;
        }


        int xid = request.getInt(XID);
        int cid = request.getInt(CUSTOMER_ID);
        int flightNumber = request.getInt(FLIGHT_NUMBER);

        JSONObject priceRequest = new JSONObject();

        priceRequest.put(TYPE, FLIGHT_ENTITY);
        priceRequest.put(ACTION, QUERY_FLIGHTS_PRICE);
        priceRequest.put(XID, xid);
        priceRequest.put(FLIGHT_NUMBER, flightNumber);


        JSONObject replyPrice = sendAndReceiveAgnostic(ServerConstants.FLIGHTS_SERVER_ADDRESS, ServerConstants.FLIGHTS_SERVER_PORT, priceRequest);

        if (replyPrice == null) {
            return false;
        }

        return customerManager.reserveFlight(xid, cid, flightNumber, replyPrice.getInt(RESULT));
    }

    public boolean reserveCar(JSONObject request) throws JSONException {
        JSONObject replyCar = sendAndReceiveAgnostic(ServerConstants.CAR_SERVER_ADDRESS, ServerConstants.CAR_SERVER_PORT, request);

        if (replyCar == null || !replyCar.getBoolean(RESULT)) {
            return false;
        }

        int xid = request.getInt(XID);
        int cid = request.getInt(CUSTOMER_ID);
        String location = request.getString(CAR_LOCATION);

        JSONObject priceRequest = new JSONObject();

        priceRequest.put(TYPE, CAR_ENTITY);
        priceRequest.put(ACTION, QUERY_CARS_PRICE);
        priceRequest.put(XID, xid);
        priceRequest.put(CAR_LOCATION, location);


        JSONObject replyPrice = sendAndReceiveAgnostic(ServerConstants.CAR_SERVER_ADDRESS, ServerConstants.CAR_SERVER_PORT, priceRequest);

        if (replyPrice == null) {
            return false;
        }

        return customerManager.reserveCar(xid, cid, location, replyPrice.getInt(RESULT));
    }

    public boolean reserveRoom(JSONObject request) throws JSONException {
        JSONObject replyRoom = sendAndReceiveAgnostic(ServerConstants.ROOMS_SERVER_ADDRESS, ServerConstants.ROOMS_SERVER_PORT, request);

        if (replyRoom == null || !replyRoom.getBoolean(RESULT)) {
            return false;
        }

        int xid = request.getInt(XID);
        int cid = request.getInt(CUSTOMER_ID);
        String location = request.getString(ROOM_LOCATION);

        JSONObject priceRequest = new JSONObject();

        priceRequest.put(TYPE, ROOM_ENTITY);
        priceRequest.put(ACTION, QUERY_ROOMS_PRICE);
        priceRequest.put(XID, xid);
        priceRequest.put(ROOM_LOCATION, location);


        JSONObject replyPrice = sendAndReceiveAgnostic(ServerConstants.ROOMS_SERVER_ADDRESS, ServerConstants.ROOMS_SERVER_PORT, priceRequest);

        if (replyPrice == null) {
            return false;
        }

        return customerManager.reserveRoom(xid, cid, location, replyPrice.getInt(RESULT));
    }

    public boolean bundle(JSONObject request) throws JSONException {

        int xid = request.getInt(XID);
        int cid = request.getInt(CUSTOMER_ID);
        String location = request.getString(ROOM_LOCATION);
        JSONArray flightNumbers = request.getJSONArray(FLIGHT_NUMBERS);

        for (int i = 0; i < flightNumbers.length(); i++) {
            try {
                String flightNumber = (String) flightNumbers.get(i);
                int parsedFlightNumber = Integer.parseInt(flightNumber);

                // TODO: DEAL WITH THE CASE OF CORRUPTED DATA IF ONE CALL FAILS AND THE OTHERS GO THROUGH IN THE NEXT ITERATION, NOT NOW THOUGH
                // it was said by TA in Discussion forum of myCourses that we do not need to deal with corrupt data for now, so it's okay
                // TODO: WILL ALSO HAVE TO IMPLEMENT "UNRESERVE" METHODS FOR THIS

                JSONObject reserveFlightRequest = RequestFactory.getReserveFlightRequest(xid, cid, parsedFlightNumber);
                if (!reserveFlight(reserveFlightRequest)) {
                    return false;
                }

            } catch (NumberFormatException e) {
                logger.severe(e.toString());
                return false;
            }
        }

        if (request.getBoolean(BOOK_CAR)) {
            JSONObject reserveCarRequest = RequestFactory.getReserveCarRequest(xid, cid, location);
            if(!reserveCar(reserveCarRequest)) {
                return false;
            }
        }

        if (request.getBoolean(BOOK_ROOM)) {
            JSONObject reserveRoomRequest = RequestFactory.getReserveRoomRequest(xid, cid, location);
            if(!reserveRoom(reserveRoomRequest)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public void start(int port) {
        SocketUtils.startServerConnection(ServerConstants.MIDDLEWARE_SERVER_ADDRESS, port, maxConcurrentClients, this);
    }
}
