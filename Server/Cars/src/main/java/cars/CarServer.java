package cars;

import Constants.ServerConstants;
import LockManager.DeadlockException;
import RM.ResourceManager;
import Tcp.IServer;
import Tcp.SocketUtils;
import Utilities.FileLogger;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.logging.Logger;

import static Constants.GeneralConstants.*;
import static Tcp.SocketUtils.sendReply;

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

        boolean result = false;
        boolean abort = false;
        int res = 0;

        switch ((String) request.get(ACTION)) {

            case ADD_CARS:
                int xid = request.getInt(CAR_XID);
                String location = request.getString(CAR_LOCATION);
                int count = request.getInt(CAR_COUNT);
                int price = request.getInt(CAR_PRICE);

                try {
                    result = addCars(xid, location, count, price);
                } catch (DeadlockException e) {
                    logger.info(e.toString());
                    abort = true;
                }
                sendReply(writer, result, abort);
                break;

            case DELETE_CARS:
                xid = request.getInt(CAR_XID);
                location = request.getString(CAR_LOCATION);
                try {
                    result = deleteCars(xid, location);
                } catch (DeadlockException e) {
                    logger.info(e.toString());
                    abort = true;
                }
                sendReply(writer, result, abort);
                break;

            case QUERY_CARS:
                xid = request.getInt(CAR_XID);
                location = request.getString(CAR_LOCATION);
                try {
                    res = queryCars(xid, location);
                } catch (DeadlockException e) {
                    logger.info(e.toString());
                    abort = true;
                }
                sendReply(writer, res, abort);
                break;

            case QUERY_CARS_PRICE:
                xid = request.getInt(CAR_XID);
                location = request.getString(CAR_LOCATION);

                try {
                    res = queryCarsPrice(xid, location);
                } catch (DeadlockException e) {
                    logger.info(e.toString());
                    abort = true;
                }
                sendReply(writer, res, abort);
                break;

            case RESERVE_CARS:
                xid = request.getInt(CAR_XID);
                int customerId = request.getInt(CAR_CUSTOMER_ID);
                location = request.getString(CAR_LOCATION);

                try {
                    result = reserveCar(xid, customerId, location);
                } catch (DeadlockException e) {
                    logger.info(e.toString());
                    abort = true;
                }
                sendReply(writer, result, abort);
                break;

            case NEW_CUSTOMER:
                xid = request.getInt(XID);

                res = newCustomer(xid);

                sendReply(writer, res);
                break;

            case NEW_CUSTOMER_ID:
                xid = request.getInt(XID);
                customerId = request.getInt(CUSTOMER_ID);

                try {
                    result = newCustomerTransaction(xid, customerId);
                } catch (DeadlockException e) {
                    logger.info(e.toString());
                    abort = true;
                }

                sendReply(writer, result, abort);
                break;

            case DELETE_CUSTOMER:
                xid = request.getInt(XID);
                customerId = request.getInt(CUSTOMER_ID);
                try {
                    result = deleteCustomerTransaction(xid, customerId);
                } catch (DeadlockException e) {
                    logger.info(e.toString());
                    abort = true;
                }

                sendReply(writer, result, abort);
                break;

            case COMMIT:
                xid = request.getInt(XID);
                result = commit(xid);
                sendReply(writer, result);
                break;

            case ABORT:
                xid = request.getInt(XID);
                result = abort(xid);
                sendReply(writer, result);
                break;
        }

    }
}
