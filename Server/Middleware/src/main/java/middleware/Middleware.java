package middleware;

import Constants.ServerConstants;
import RM.IResourceManager;
import RM.ResourceManager;
import Tcp.IServer;
import Tcp.SocketUtils;
import com.sun.corba.se.spi.activation.Server;
import customer.CustomerResourceManager;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Vector;

import static Constants.GeneralConstants.*;
import static Tcp.SocketUtils.sendReply;

public class Middleware extends ResourceManager implements IServer {
    private static final String serverName = "Middleware";
    private static final int maxConcurrentClients = 10;
    public CustomerResourceManager customerManager;


    public static void main(String[] args) {
        Middleware obj = new Middleware();
        obj.start(ServerConstants.MIDDLEWARE_PORT);
    }

    public Middleware() {
        super(serverName);
        customerManager = new CustomerResourceManager();
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
                    case RESERVE_FLIGHTS:
                        boolRes = reserveFlight(request);
                        sendReply(writer, boolRes);
                        break;
                    case RESERVE_ROOMS:
                        boolRes = reserveRoom(request);
                        sendReply(writer, boolRes);
                        break;
                    case BUNDLE:
                        // TODO: Implement
                        boolRes = bundle(0, 0, null, null, false, false);
                        sendReply(writer, boolRes);
                        break;
                }
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
    private JSONObject sendAndReceiveAgnostic(String serverAddress, int port, JSONObject request) {
        JSONObject result = null;

        try {
            Socket server = new Socket(InetAddress.getByName(serverAddress), port);
            System.out.println("Sending request to server: " + serverAddress + ":" + port);
            OutputStreamWriter writer = new OutputStreamWriter(server.getOutputStream(), CHAR_SET);
            BufferedReader reader = new BufferedReader(new InputStreamReader(server.getInputStream(), CHAR_SET));

            result = sendAndReceive(request, writer, reader);

            server.close();
            writer.close();
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    private JSONObject sendAndReceive(JSONObject request, OutputStreamWriter writer, BufferedReader reader) {
        try {
            writer.write(request.toString() + "\n");
            writer.flush();

            String line = reader.readLine();
            System.out.println("Reply from server: " + line + "\n");
            JSONObject reply = new JSONObject(line);
            return reply;
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

        return null;
    }


    public int newCustomer(JSONObject request) throws RemoteException, JSONException {
        int xid = request.getInt(CUSTOMER_XID);

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


        return customerManager.newCustomer(xid) == 1 && replyFlights.getInt(RESULT) == 1 && replyCar.getInt(RESULT) == 1 && replyRooms.getInt(RESULT) == 1 ? 1 : 0;
    }

    public boolean newCustomerId(JSONObject request) throws RemoteException, JSONException {
        int xid = request.getInt(CUSTOMER_XID);
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

    public boolean deleteCustomer(JSONObject request) throws RemoteException, JSONException {
        int xid = request.getInt(CUSTOMER_XID);
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


    public String queryCustomerInfo(JSONObject request) throws RemoteException, JSONException {
        int xid = request.getInt(CUSTOMER_XID);
        int cid = request.getInt(CUSTOMER_ID);

        return customerManager.queryCustomerInfo(xid, cid);
    }


    public boolean reserveFlight(JSONObject request) throws RemoteException, JSONException {
        JSONObject replyFlights = sendAndReceiveAgnostic(ServerConstants.FLIGHTS_SERVER_ADDRESS, ServerConstants.FLIGHTS_SERVER_PORT, request);

        if (replyFlights == null || !replyFlights.getBoolean(RESULT)) {
            return false;
        }


        int xid = request.getInt(CUSTOMER_XID);
        int cid = request.getInt(CUSTOMER_ID);
        int flightNumber = request.getInt(FLIGHT_NUMBER);

        JSONObject priceRequest = new JSONObject();

        priceRequest.put(TYPE, FLIGHT_ENTITY);
        priceRequest.put(ACTION, QUERY_FLIGHTS_PRICE);
        priceRequest.put(CUSTOMER_XID, xid);
        priceRequest.put(FLIGHT_NUMBER, flightNumber);


        JSONObject replyPrice = sendAndReceiveAgnostic(ServerConstants.FLIGHTS_SERVER_ADDRESS, ServerConstants.FLIGHTS_SERVER_PORT, priceRequest);

        if (replyPrice == null) {
            return false;
        }

        return customerManager.reserveFlight(xid, cid, flightNumber, replyPrice.getInt(RESULT));
    }

    public boolean reserveCar(JSONObject request) throws RemoteException, JSONException {
        JSONObject replyCar = sendAndReceiveAgnostic(ServerConstants.CAR_SERVER_ADDRESS, ServerConstants.CAR_SERVER_PORT, request);

        if (replyCar == null || !replyCar.getBoolean(RESULT)) {
            return false;
        }

        int xid = request.getInt(CUSTOMER_XID);
        int cid = request.getInt(CUSTOMER_ID);
        String location = request.getString(CAR_LOCATION);

        JSONObject priceRequest = new JSONObject();

        priceRequest.put(TYPE, CAR_ENTITY);
        priceRequest.put(ACTION, QUERY_CARS_PRICE);
        priceRequest.put(CUSTOMER_XID, xid);
        priceRequest.put(CAR_LOCATION, location);


        JSONObject replyPrice = sendAndReceiveAgnostic(ServerConstants.CAR_SERVER_ADDRESS, ServerConstants.CAR_SERVER_PORT, priceRequest);

        if (replyPrice == null) {
            return false;
        }

        return customerManager.reserveCar(xid, cid, location, replyPrice.getInt(RESULT));
    }

    public boolean reserveRoom(JSONObject request) throws RemoteException, JSONException {
        JSONObject replyRoom = sendAndReceiveAgnostic(ServerConstants.ROOMS_SERVER_ADDRESS, ServerConstants.ROOMS_SERVER_PORT, request);

        if (replyRoom == null || !replyRoom.getBoolean(RESULT)) {
            return false;
        }

        int xid = request.getInt(CUSTOMER_XID);
        int cid = request.getInt(CUSTOMER_ID);
        String location = request.getString(ROOM_LOCATION);

        JSONObject priceRequest = new JSONObject();

        priceRequest.put(TYPE, ROOM_ENTITY);
        priceRequest.put(ACTION, QUERY_ROOMS_PRICE);
        priceRequest.put(CUSTOMER_XID, xid);
        priceRequest.put(ROOM_LOCATION, location);


        JSONObject replyPrice = sendAndReceiveAgnostic(ServerConstants.ROOMS_SERVER_ADDRESS, ServerConstants.ROOMS_SERVER_PORT, priceRequest);

        if (replyPrice == null) {
            return false;
        }

        return customerManager.reserveRoom(xid, cid, location, replyPrice.getInt(RESULT));
    }

    // TODO: IMPLEMENT THIS
    public boolean bundle(int id, int customerID, Vector<String> flightNumbers, String location, boolean car, boolean room) throws RemoteException {
//        for (String flightNumber : flightNumbers) {
//            try {
//                int parsedFlightNumber = Integer.parseInt(flightNumber);
//
//                // TODO: DEAL WITH THE CASE OF CORRUPTED DATA IF ONE CALL FAILS AND THE OTHERS GO THROUGH IN THE NEXT ITERATION, NOT NOW THOUGH
//                // it was said by TA in Discussion forum of myCourses that we do not need to deal with corrupt data for now, so it's okay
//                // TODO: WILL ALSO HAVE TO IMPLEMENT "UNRESERVE" METHODS FOR THIS
//
//                if (!reserveFlight(id, customerID, parsedFlightNumber)) {
//                    return false;
//                }
//
//            } catch (NumberFormatException e) {
//                System.out.println(e);
//                return false;
//            }
//        }
//
//        if (car && !reserveCar(id, customerID, location)) {
//            return false;
//        }
//
//        return room ? reserveRoom(id, customerID, location) : true;

        return false;
    }

    @Override
    public String getName() throws RemoteException {
        return null;
    }


    @Override
    public void start(int port) {
        SocketUtils.startServerConnection(ServerConstants.MIDDLEWARE_SERVER_ADDRESS, port, maxConcurrentClients, this);
    }
}
