package middleware;

import Constants.ServerConstants;
import Constants.TransactionConstants.STATUS;
import LockManager.DeadlockException;
import Persistence.PersistedFile;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static Constants.GeneralConstants.*;
import static Constants.ServerConstants.*;
import static TCP.RequestFactory.getDecisionRequest;
import static TCP.RequestFactory.getVoteRequest;
import static TCP.SocketUtils.sendReply;
import static TCP.SocketUtils.sendReplyToClient;
import static TCP.SocketUtils.sendRequest;
import static Utilities.RMNameServerUtil.nameToHost;
import static Utilities.RMNameServerUtil.nameToPort;

public class Middleware implements IServer {
    private static final String serverName = "Middleware";
    private static final int maxConcurrentClients = 10;
    public CustomerResourceManager customerManager;
    private XIDManager xIDManager;
    public Map<Integer, Timer> timers;

    /**
     * 0. No crash
     * 1. Crash before sending vote request
     * 2. Crash after sending vote request and before receiving any replies
     * 3. Crash after receiving some replies but not all
     * 4. Crash after receiving all replies but before deciding
     * 5. Crash after deciding but before sending decision
     * 6. Crash after sending some but not all decisions
     * 7. Crash after having sent all decisions
     * 8. Crash during recovery of the coordinator
     */
    private AtomicInteger middlewareCrashMode;

    private PersistedFile<Boolean> crashDuringRecoveryStatus;

    private static final Logger logger = FileLogger.getLogger(Middleware.class);

    public static void main(String[] args) {
        Middleware obj = new Middleware();
        obj.start(ServerConstants.MIDDLEWARE_PORT);
    }

    public Middleware() {
        customerManager = new CustomerResourceManager();
        xIDManager = new XIDManager();
        timers = new ConcurrentHashMap<>();
        crashDuringRecoveryStatus = new PersistedFile<>(serverName, CRASH_AT_RECOVERY_FLAG);
        initCrashMode();
        loadData();
    }

    private void initCrashMode() {
        middlewareCrashMode = new AtomicInteger(0);

        //set crash mode to 8 if we need middleware to crash during recovery
        if (crashDuringRecoveryStatus.exists()) {
            try {
                if (crashDuringRecoveryStatus.read()) {
                    middlewareCrashMode.set(8);
                    crashDuringRecoveryStatus.save(false); //so that it doesn't crash again at the next restart
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private void loadData() {
        xIDManager.getActiveTransactions().keySet().forEach(key -> {
            logger.info("Restoring XID=" + key + " with status " + xIDManager.getActiveTransactions().get(key).getStatus().toString());

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
                case COMMITTED:
                    sendDecision(key, true);
                    break;
                default:
                    break;
            }
            if (middlewareCrashMode.get() == 8) {
                //Crash during recovery of the coordinator
                logger.info("Simulating middleware crash mode=" + middlewareCrashMode);
                System.exit(1);
            }
        });

        //still crash if there were no active transactions
        if (middlewareCrashMode.get() == 8) {
            //Crash during recovery of the coordinator
            logger.info("Simulating middleware crash mode=" + middlewareCrashMode);
            System.exit(1);
        }
    }

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

                    case GET_DECISION:
                        int xid = request.getInt(XID);
                        boolRes = getDecision(xid);
                        sendReply(writer, boolRes);
                        break;

                    case HAVE_COMMITTED:
                        xid = request.getInt(XID);
                        String rm = request.getString(RM_ADDRESS);

                        processHaveCommitted(xid, rm);
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

            case CRASH:
                switch ((String) request.get(ACTION)) {
                    case CRASH_MIDDLEWARE:
                        int mode = request.getInt(CRASH_MODE);
                        logger.info("Enable middleware crash mode=" + mode);
                        middlewareCrashMode.set(mode);
                        if (mode == 8) {
                            crashDuringRecoveryStatus.save(true);
                        } else {
                            crashDuringRecoveryStatus.save(false);
                        }
                        break;

                    case CRASH_RESOURCE_MANAGER:
                        String rm = request.getString(RESOURCE_MANAGER_NAME);
                        mode = request.getInt(CRASH_MODE);
                        logger.info("Enable resource manager crash mode=" + mode);
                        sendRequest(nameToHost(rm), nameToPort(rm), request);
                        break;

                    case RESET_CRASHES:
                        logger.info("Resetting all crash modes");
                        sendRequest(ServerConstants.CAR_SERVER_ADDRESS, ServerConstants.CAR_SERVER_PORT, request);
                        sendRequest(ServerConstants.ROOMS_SERVER_ADDRESS, ServerConstants.ROOMS_SERVER_PORT, request);
                        sendRequest(ServerConstants.FLIGHTS_SERVER_ADDRESS, ServerConstants.FLIGHTS_SERVER_PORT, request);
                        middlewareCrashMode.set(0);
                        crashDuringRecoveryStatus.save(false);
                        break;
                }
                break;
        }
    }

    private boolean voteRequest(int xid) {
        Set<String> rms = xIDManager.activeTransactions.get(xid).getParticipants();
        xIDManager.activeTransactions.get(xid).setStatus(STATUS.PREPARED);
        xIDManager.persistData();
        logger.info("Sending vote request to " + rms);

        if (middlewareCrashMode.get() == 1) {
            //Crash before sending vote request
            logger.info("Simulating middleware crash mode=" + middlewareCrashMode);
            System.exit(1);
        }

        int replies = 0;

        for (String rm : rms) {
            String[] hostPort = rm.split(":");
            String host = hostPort[0];

            try {
                JSONObject request = getVoteRequest(xid);
                JSONObject result = sendAndReceiveAgnostic(host, Integer.parseInt(hostPort[1]), request, true);

                replies++;

                if (middlewareCrashMode.get() == 2) {
                    //Crash after sending vote request and before receiving any replies
                    //Simulating by ignoring all replies
                    continue;
                }

                if (middlewareCrashMode.get() == 3) {
                    //Crash after receiving some replies but not all
                    //Simulating by ignoring some replies
                    if (replies > 1) {
                        continue;
                    }
                }

                // retry once to wait for vote
                // transaction becomes blocked
                // if second time is still bad, then
                if (result == null || !result.getBoolean(RESULT)) {
                    logger.info("Failed to acquire vote attempt");
                    throw new JSONException("Invalid");
                }

            } catch (JSONException | NullPointerException | DeadlockException e) {
                logger.info("Returned false from voteRequest");
                e.printStackTrace();
                return false;
            }
        }

        logger.info("Successfully completed voteRequest");

        if (middlewareCrashMode.get() == 2 || middlewareCrashMode.get() == 3 || middlewareCrashMode.get() == 4) {
            //2- Crash after sending vote request and before receiving any replies (simulated by ignoring all replies. see above)
            //3- Crash after receiving some replies but not all (simulated by ignoring some replies. see above)
            //4- Crash after receiving all replies but before deciding
            logger.info("Simulating middleware crash mode=" + middlewareCrashMode);
            System.exit(1);
        }
        return true;
    }

    private void sendDecision(int xid, boolean decision) {
        Set<String> rms = xIDManager.activeTransactions.get(xid).getParticipants();
        xIDManager.activeTransactions.get(xid).setStatus((decision) ? STATUS.COMMITTED : STATUS.ABORTED);
        xIDManager.persistData();

        if (middlewareCrashMode.get() == 5) {
            //Crash after deciding but before sending decision
            logger.info("Simulating middleware crash mode=" + middlewareCrashMode);
            System.exit(1);
        }

        logger.info("Sending decision to " + rms);

        for (String rm : rms) {
            String[] hostPort = rm.split(":");
            String host = hostPort[0];

            try {
                JSONObject request = getDecisionRequest(xid, decision);
                sendRequest(host, Integer.parseInt(hostPort[1]), request);


            } catch (JSONException e) {
                logger.info("Returned false from sendDecision");
                e.printStackTrace();
            }

            if (middlewareCrashMode.get() == 6) {
                //Crash after sending some but not all decisions
                logger.info("Simulating middleware crash mode=" + middlewareCrashMode);
                System.exit(1);
            }
        }

        if (decision) {
            customerManager.commit(xid);
        } else {
            customerManager.abort(xid);
        }

        if (middlewareCrashMode.get() == 7) {
            //Crash after having sent all decisions
            logger.info("Simulating middleware crash mode=" + middlewareCrashMode);
            System.exit(1);
        }

        if (rms.size() == 0) {
            xIDManager.completeTransaction(xid);
        }
    }

    private boolean commitAll(JSONObject commitRequest) throws JSONException {
        int xid = commitRequest.getInt(XID);
        timers.remove(xid);
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

    private boolean getDecision(int xid) {
        boolean reply = false;
        if (xIDManager.getActiveTransactions().get(xid) != null) {
            reply = xIDManager.getActiveTransactions().get(xid).getStatus() == STATUS.COMMITTED;
        }

        return reply;
    }

    private void processHaveCommitted(int xid, String rmAddress) {
        if (xIDManager.getActiveTransactions().get(xid) != null) {
            xIDManager.getActiveTransactions().get(xid).getParticipants().remove(rmAddress);

            if (xIDManager.getActiveTransactions().get(xid).getParticipants().size() == 0) {
                xIDManager.completeTransaction(xid);
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


        //verify all flights available
        for (int i = 0; i < flightNumbers.length(); i++) {
            try {
                String flightNumber = (String) flightNumbers.get(i);
                int parsedFlightNumber = Integer.parseInt(flightNumber);

                JSONObject queryFlightRequest = RequestFactory.getQueryFlightRequest(xid, parsedFlightNumber);
                JSONObject result = sendAndReceiveAgnostic(ServerConstants.FLIGHTS_SERVER_ADDRESS, ServerConstants.FLIGHTS_SERVER_PORT, queryFlightRequest);

                if (result.getInt(RESULT) < 1) {
                    logger.info("Flight " + flightNumber + " not available. Canceling bundle request entirely.");
                    return false;
                }

            } catch (NumberFormatException e) {
                logger.severe(e.toString());
                return false;
            }
        }

        //verify car available
        if (request.getBoolean(BOOK_CAR)) {
            JSONObject queryCarRequest = RequestFactory.getQueryCarRequest(xid, location);
            JSONObject result = sendAndReceiveAgnostic(ServerConstants.CAR_SERVER_ADDRESS, ServerConstants.CAR_SERVER_PORT, queryCarRequest);

            if (result.getInt(RESULT) < 1) {
                logger.info("Car not available. Canceling bundle request entirely.");
                return false;
            }
        }

        //verify room available
        if (request.getBoolean(BOOK_ROOM)) {
            JSONObject queryRoomRequest = RequestFactory.getQueryRoomRequest(xid, location);
            JSONObject result = sendAndReceiveAgnostic(ServerConstants.ROOMS_SERVER_ADDRESS, ServerConstants.ROOMS_SERVER_PORT, queryRoomRequest);

            if (result.getInt(RESULT) < 1) {
                logger.info("Room not available. Canceling bundle request entirely.");
                return false;
            }
        }


        // make reservations
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

            logger.info("Created new timer for xid=" + id);

            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (xIDManager.getActiveTransactions().containsKey(id)) {
                        try {
                            logger.info("Timer expiring for xid=" + id);

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
