package cars;

import Constants.ServerConstants;
import LockManager.DeadlockException;
import RM.ResourceManager;
import TCP.IServer;
import TCP.SocketUtils;
import Utilities.FileLogger;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.logging.Logger;

import static Constants.GeneralConstants.*;
import static TCP.SocketUtils.sendReply;

public class CarServer extends ResourceManager implements IServer {

    private static final String serverName = "Cars";
    private static final int maxConcurrentClients = 10;

    private static final Logger logger = FileLogger.getLogger(CarServer.class);

    CarServer() {
        super(serverName);
    }

    @Override
    public void start(int port) {
        SocketUtils.startServerConnection(ServerConstants.CAR_SERVER_ADDRESS, port, maxConcurrentClients, this);
    }

    @Override
    public void handleRequest(JSONObject request, OutputStreamWriter writer) throws IOException, JSONException {

        logger.info("Received request " + request);

        boolean result = false;
        boolean deadlock = false;
        int res = 0;

        switch ((String) request.get(ACTION)) {

            case ADD_CARS:
                int xid = request.getInt(XID);
                String location = request.getString(CAR_LOCATION);
                int count = request.getInt(CAR_COUNT);
                int price = request.getInt(CAR_PRICE);

                try {
                    result = addCars(xid, location, count, price);
                } catch (DeadlockException e) {
                    logger.info(e.toString());
                    deadlock = true;
                }
                sendReply(writer, result, deadlock);
                break;

            case DELETE_CARS:
                xid = request.getInt(XID);
                location = request.getString(CAR_LOCATION);
                try {
                    result = deleteCars(xid, location);
                } catch (DeadlockException e) {
                    logger.info(e.toString());
                    deadlock = true;
                }
                sendReply(writer, result, deadlock);
                break;

            case QUERY_CARS:
                xid = request.getInt(XID);
                location = request.getString(CAR_LOCATION);
                try {
                    res = queryCars(xid, location);
                } catch (DeadlockException e) {
                    logger.info(e.toString());
                    deadlock = true;
                }
                sendReply(writer, res, deadlock);
                break;

            case QUERY_CARS_PRICE:
                xid = request.getInt(XID);
                location = request.getString(CAR_LOCATION);

                try {
                    res = queryCarsPrice(xid, location);
                } catch (DeadlockException e) {
                    logger.info(e.toString());
                    deadlock = true;
                }
                sendReply(writer, res, deadlock);
                break;

            case RESERVE_CARS:
                xid = request.getInt(XID);
                int customerId = request.getInt(CAR_CUSTOMER_ID);
                location = request.getString(CAR_LOCATION);

                try {
                    result = reserveCar(xid, customerId, location);
                } catch (DeadlockException e) {
                    logger.info(e.toString());
                    deadlock = true;
                }
                sendReply(writer, result, deadlock);
                break;

            case NEW_CUSTOMER_ID:
                xid = request.getInt(XID);
                customerId = request.getInt(CUSTOMER_ID);

                try {
                    result = newCustomer(xid, customerId);
                } catch (DeadlockException e) {
                    logger.info(e.toString());
                    deadlock = true;
                }

                sendReply(writer, result, deadlock);
                break;

            case DELETE_CUSTOMER:
                xid = request.getInt(XID);
                customerId = request.getInt(CUSTOMER_ID);
                try {
                    result = deleteCustomer(xid, customerId);
                } catch (DeadlockException e) {
                    logger.info(e.toString());
                    deadlock = true;
                }

                sendReply(writer, result, deadlock);
                break;

            case COMMIT:
                xid = request.getInt(XID);
                commit(xid);
                break;

            case ABORT:
                xid = request.getInt(XID);
                abort(xid);
                break;

            case SHUTDOWN:
                logger.info("Shutting down");
                System.exit(0);
                break;

            case VOTE_REQUEST:
                xid = request.getInt(XID);
                result = voteReply(xid);
                sendReply(writer, result, deadlock);

                if (!result) {
                    abort(xid);
                }

                break;

            case GET_DECISION:
                xid = request.getInt(XID);
                boolean type = request.getBoolean(DECISION_FIELD);

                if(type) {
                    result = commit(xid);
                } else {
                    result = abort(xid);
                }

                sendReply(writer, result, deadlock);

                break;
        }

    }
}
