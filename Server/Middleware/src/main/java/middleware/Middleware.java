package middleware;

import Constants.ServerConstants;
import Constants.TransactionConstants.STATUS;
import LockManager.DeadlockException;
import RM.ResourceManager;
import TCP.IServer;
import TCP.RequestFactory;
import TCP.SocketUtils;
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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static Constants.GeneralConstants.*;
import static Constants.ServerConstants.MIDDLEWARE_SERVER_ADDRESS;
import static TCP.RequestFactory.getDecisionRequest;
import static TCP.RequestFactory.getVoteRequest;
import static TCP.SocketUtils.sendReply;
import static TCP.SocketUtils.sendReplyToClient;

public class Middleware extends ResourceManager implements IServer {
    private static final String serverName = "Middleware";
    private static final int maxConcurrentClients = 10;
    public CustomerResourceManager customerManager;
    private XIDManager xIDManager;
    public Map<Integer, Timer> timers;

    private static final Logger logger = FileLogger.getLogger(Middleware.class);

    public static void main(String[] args) {
        Middleware obj = new Middleware();
        obj.start(ServerConstants.MIDDLEWARE_PORT);
    }

    public Middleware() {
        super(serverName);
        customerManager = new CustomerResourceManager();
        xIDManager = new XIDManager();
        timers = new ConcurrentHashMap<>();
        loadData();
    }

    private void loadData() {
        xIDManager.getActiveTransactions().keySet().forEach(key -> {
            switch (xIDManager.getActiveTransactions().get(key).getStatus()) {
                case ACTIVE:
                    resetTimeout(key);
                    break;
                case ABORTED:
                case PREPARED:
                    try {
                        abortAll(key);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
                case COMMMITTED:
                    sendDecision(key, true);
                    break;
                default:
                    break;
            }
        });
    }

    @Override
    public void handleRequest(JSONObject request, OutputStreamWriter writer) throws IOException, JSONException {

        logger.info("Received request " + request);

        boolean boolRes = false;
        boolean aborted = false;
        JSONObject res = null;

        if (!xIDManager.validate(request)) {
            JSONObject reply = new JSONObject();
            reply.put(VALID_XID, false);
            sendReplyToClient(writer, reply, false);
            return;
        }

        if (request.has(XID)) {
            resetTimeout(request.getInt(XID));
        }

        switch ((String) request.get(TYPE)) {
            // redirection happens here
            case CAR_ENTITY:
                try {
                    res = sendAndReceiveAgnostic(ServerConstants.CAR_SERVER_ADDRESS, ServerConstants.CAR_SERVER_PORT, request);
                } catch (DeadlockException e) {
                    abortAll(request.getInt(XID));
                    aborted = true;
                }
                sendReplyToClient(writer, res, aborted);
                break;
            case FLIGHT_ENTITY:
                try {
                    res = sendAndReceiveAgnostic(ServerConstants.FLIGHTS_SERVER_ADDRESS, ServerConstants.FLIGHTS_SERVER_PORT, request);
                } catch (DeadlockException e) {
                    abortAll(request.getInt(XID));
                    aborted = true;
                }
                sendReplyToClient(writer, res, aborted);
                break;
            case ROOM_ENTITY:
                try {
                    res = sendAndReceiveAgnostic(ServerConstants.ROOMS_SERVER_ADDRESS, ServerConstants.ROOMS_SERVER_PORT, request);
                } catch (DeadlockException e) {
                    abortAll(request.getInt(XID));
                    aborted = true;
                }
                sendReplyToClient(writer, res, aborted);
                break;
            case CUSTOMER_ENTITY:
                switch ((String) request.get(ACTION)) {
                    case NEW_CUSTOMER:
                        int result = 0;
                        try {
                            result = newCustomer(request);
                        } catch (DeadlockException e) {
                            abortAll(request.getInt(XID));
                            aborted = true;
                        }
                        sendReplyToClient(writer, result, aborted);
                        break;
                    case NEW_CUSTOMER_ID:

                        try {
                            boolRes = newCustomerId(request);
                        } catch (DeadlockException e) {
                            abortAll(request.getInt(XID));
                            aborted = true;
                        }
                        sendReplyToClient(writer, boolRes, aborted);
                        break;
                    case DELETE_CUSTOMER:
                        try {
                            boolRes = deleteCustomer(request);
                        } catch (DeadlockException e) {
                            abortAll(request.getInt(XID));
                            aborted = true;
                        }
                        sendReplyToClient(writer, boolRes, aborted);
                        break;
                    case QUERY_CUSTOMER:
                        String strRes = null;
                        try {
                            strRes = queryCustomerInfo(request);
                        } catch (DeadlockException e) {
                            abortAll(request.getInt(XID));
                            aborted = true;
                        }
                        sendReplyToClient(writer, strRes, aborted);
                        break;
                    case RESERVE_CARS:
                        try {
                            boolRes = reserveCar(request);
                        } catch (DeadlockException e) {
                            abortAll(request.getInt(XID));
                            aborted = true;
                        }
                        sendReplyToClient(writer, boolRes, aborted);
                        break;
                    case RESERVE_FLIGHT:
                        try {
                            boolRes = reserveFlight(request);
                        } catch (DeadlockException e) {
                            abortAll(request.getInt(XID));
                            aborted = true;
                        }
                        sendReply(writer, boolRes, aborted);
                        break;
                    case RESERVE_ROOMS:
                        try {
                            boolRes = reserveRoom(request);
                        } catch (DeadlockException e) {
                            abortAll(request.getInt(XID));
                            aborted = true;
                        }
                        sendReplyToClient(writer, boolRes, aborted);
                        break;
                    case BUNDLE:
                        try {
                            boolRes = bundle(request);
                        } catch (DeadlockException e) {
                            abortAll(request.getInt(XID));
                            aborted = true;
                        }
                        sendReplyToClient(writer, boolRes, aborted);
                        break;
                }
                break;

            case TRANSACTION:
                switch ((String) request.get(ACTION)) {

                    case NEW_TRANSACTION:
                        int result = newTransactionWrapper();
                        sendReply(writer, result);
                        break;

                    case COMMIT:
                        boolRes = commitAll(request);
                        sendReply(writer, boolRes);
                        break;

                    case ABORT:
                        abortAll(request.getInt(XID));
                        sendReply(writer, true);
                        break;

                    case VOTE_REQUEST:
                        int xid = request.getInt(XID);
                        boolRes = voteReply(xid);
                        sendReply(writer, boolRes);

                        if (!boolRes) {
                            abort(xid);
                        }

                        break;

                    case DECISION:
                        xid = request.getInt(XID);
                        boolean type = request.getBoolean(DECISION_FIELD);

                        if (type) {
                            boolRes = commit(xid);
                        } else {
                            boolRes = abort(xid);
                        }

                        sendReply(writer, boolRes);

                        break;
                }
                break;

            case OTHERS:
                switch ((String) request.get(ACTION)) {
                    case SHUTDOWN:
                        sendRequest(ServerConstants.CAR_SERVER_ADDRESS, ServerConstants.CAR_SERVER_PORT, request);
                        sendRequest(ServerConstants.ROOMS_SERVER_ADDRESS, ServerConstants.ROOMS_SERVER_PORT, request);
                        sendRequest(ServerConstants.FLIGHTS_SERVER_ADDRESS, ServerConstants.FLIGHTS_SERVER_PORT, request);
                        logger.info("Shutting down");
                        System.exit(0);
                        break;
                }
                break;
        }
    }

    private boolean voteRequest(int xid) {
        Set<String> rms = xIDManager.activeTransactions.get(xid).getParticipants();
        xIDManager.activeTransactions.get(xid).setStatus(STATUS.PREPARED);
        logger.info("Sending vote request to " + rms);

        for (String rm : rms) {
            String[] hostPort = rm.split(":");
            String host = hostPort[0];

            try {
                JSONObject request = getVoteRequest(xid);
                JSONObject result = sendAndReceiveAgnostic(host, Integer.parseInt(hostPort[1]), request, true);

                if (result == null || !result.getBoolean(RESULT)) {
                    logger.info("Returned false from voteRequest because of bad request");

                    throw new JSONException("Invalid");
                }

            } catch (JSONException | DeadlockException e) {
                logger.info("Returned false from voteRequest");
                e.printStackTrace();
                return false;
            }
        }

        logger.info("Successfully completed voteRequest");
        return true;
    }

    private void sendDecision(int xid, boolean decision) {
        Set<String> rms = xIDManager.activeTransactions.get(xid).getParticipants();
        xIDManager.activeTransactions.get(xid).setStatus((decision) ? STATUS.COMMMITTED : STATUS.ABORTED);
        logger.info("Sending vote request to " + rms);

        for (String rm : rms) {
            String[] hostPort = rm.split(":");
            String host = hostPort[0];

            try {
                JSONObject request = getDecisionRequest(xid, decision);
                JSONObject result = sendAndReceiveAgnostic(host, Integer.parseInt(hostPort[1]), request, true);

                if (result == null || !result.getBoolean(RESULT)) {
                    throw new JSONException("Invalid");
                }

            } catch (JSONException | DeadlockException e) {
                logger.info("Returned false from sendDecision");
                e.printStackTrace();
            }
        }

        if (decision) {
            customerManager.commit(xid);
        } else {
            customerManager.abort(xid);
            timers.remove(xid);
        }

        xIDManager.completeTransaction(xid);
    }


    private boolean commitAll(JSONObject commitRequest) throws JSONException {
        int xid = commitRequest.getInt(XID);
        boolean decision = voteRequest(xid);
        sendDecision(xid, decision);
        return decision;
    }

    private void abortAll(int xid) throws JSONException {
        logger.info("Aborting transaction: " + xid);
        JSONObject abortRequest = RequestFactory.getAbortRequest(xid);
        Set<String> rms = xIDManager.completeTransaction(xid);
        sendRequestToRMs(abortRequest, rms);
        customerManager.abort(xid);
        timers.remove(xid);
    }

    private void sendRequestToRMs(JSONObject request, Set<String> rms) {
        logger.info("Sending request to resource managers.");
        rms.forEach(rm -> {
            String[] hostPort = rm.split(":");
            String host = hostPort[0];
            int port = Integer.parseInt(hostPort[1]);
            sendRequest(host, port, request);
        });
    }

    /**
     * Routing happens here
     *
     * @param serverAddress
     * @param port
     * @param request
     * @return
     */
    private JSONObject sendAndReceiveAgnostic(String serverAddress, int port, JSONObject request, Boolean... isTransactionOperation) throws JSONException, DeadlockException {
        JSONObject result = null;
        int xid = request.getInt(XID);

        if (isTransactionOperation.length == 0) {
            xIDManager.addRM(xid, serverAddress + ":" + port);
        }

        try {
            logger.info("Sending request " + request + " to server: " + serverAddress + ":" + port);
            Socket server = new Socket(InetAddress.getByName(serverAddress), port);
            OutputStreamWriter writer = new OutputStreamWriter(server.getOutputStream(), CHAR_SET);
            BufferedReader reader = new BufferedReader(new InputStreamReader(server.getInputStream(), CHAR_SET));

            result = SocketUtils.sendAndReceive(request, writer, reader);

            server.close();
            writer.close();
            reader.close();

            if (result.has(DEADLOCK) && result.getBoolean(DEADLOCK)) {
                throw new DeadlockException(xid, "");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    private void sendRequest(String serverAddress, int port, JSONObject request) {

        try {
            logger.info("Sending request " + request + " to server: " + serverAddress + ":" + port);
            Socket server = new Socket(InetAddress.getByName(serverAddress), port);
            OutputStreamWriter writer = new OutputStreamWriter(server.getOutputStream(), CHAR_SET);

            SocketUtils.send(request, writer);

            server.close();
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int newCustomer(JSONObject request) throws JSONException, DeadlockException {
        int xid = request.getInt(XID);
        int cid = customerManager.generateCID(xid);
        newCustomerId(RequestFactory.getAddCustomerIdRequest(xid, cid));
        return cid;
    }

    public boolean newCustomerId(JSONObject request) throws JSONException, DeadlockException {
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

    public boolean deleteCustomer(JSONObject request) throws JSONException, DeadlockException {
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


    public String queryCustomerInfo(JSONObject request) throws JSONException, DeadlockException {
        int xid = request.getInt(XID);
        int cid = request.getInt(CUSTOMER_ID);

        return customerManager.queryCustomerInfo(xid, cid);
    }


    public boolean reserveFlight(JSONObject request) throws JSONException, DeadlockException {
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

    public boolean reserveCar(JSONObject request) throws JSONException, DeadlockException {
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

    public boolean reserveRoom(JSONObject request) throws JSONException, DeadlockException {
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

    public boolean bundle(JSONObject request) throws JSONException, DeadlockException {

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
            if (!reserveCar(reserveCarRequest)) {
                return false;
            }
        }

        if (request.getBoolean(BOOK_ROOM)) {
            JSONObject reserveRoomRequest = RequestFactory.getReserveRoomRequest(xid, cid, location);
            if (!reserveRoom(reserveRoomRequest)) {
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
        SocketUtils.startServerConnection(MIDDLEWARE_SERVER_ADDRESS, port, maxConcurrentClients, this);
    }

    public int newTransactionWrapper() {
        int result = xIDManager.newTransaction();
        resetTimeout(result);

        return result;
    }

    public void resetTimeout(int id) {
        if (xIDManager.getActiveTransactions().containsKey(id)) {
            // we cancel the previous timer
            if (timers.containsKey(id)) {
                timers.get(id).cancel();
            }

            Timer timer = new Timer();

            logger.info("created new timer");

            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (xIDManager.getActiveTransactions().containsKey(id)) {
                        try {
                            abortAll(id);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }, TIMER_DELAY);

            timers.put(id, timer);
        }
    }
}
