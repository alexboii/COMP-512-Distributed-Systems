package flights;

import Constants.ServerConstants;
import LockManager.DeadlockException;
import RM.ResourceManager;
import Tcp.IServer;
import Tcp.SocketUtils;
import Utilities.FileLogger;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.logging.Logger;

import static Constants.GeneralConstants.*;
import static Tcp.SocketUtils.sendReply;


/**
 * Created by alex on 9/25/18.
 */
public class FlightsServer extends ResourceManager implements IServer {
    private static final String serverName = "Flights";
    private static final int maxConcurrentClients = 10;

    private static final Logger logger = FileLogger.getLogger(FlightsServer.class);

    public FlightsServer() {
        super(serverName);
    }

    @Override
    public void start(int port) {
        SocketUtils.startServerConnection(ServerConstants.FLIGHTS_SERVER_ADDRESS, port, maxConcurrentClients, this);
    }

    @Override
    public void handleRequest(JSONObject request, OutputStreamWriter writer) throws IOException, JSONException {

        logger.info("Received request " + request);

        boolean result = false;
        boolean deadlock = false;
        int res = 0;

        switch ((String) request.get(ACTION)) {
            case ADD_FLIGHTS:
                int xid = request.getInt(XID);
                int number = request.getInt(FLIGHT_NUMBER);
                int count = request.getInt(FLIGHT_SEATS);
                int price = request.getInt(FLIGHT_PRICE);

                try {
                    result = addFlight(xid, number, count, price);
                } catch (DeadlockException e) {
                    logger.info(e.toString());
                    deadlock = true;
                }
                sendReply(writer, result, deadlock);
                break;

            case DELETE_FLIGHTS:
                xid = request.getInt(XID);
                number = request.getInt(FLIGHT_NUMBER);

                try {
                    result = deleteFlight(xid, number);
                } catch (DeadlockException e) {
                    logger.info(e.toString());
                    deadlock = true;
                }
                sendReply(writer, result, deadlock);
                break;

            case QUERY_FLIGHTS:
                xid = request.getInt(XID);
                number = request.getInt(FLIGHT_NUMBER);

                try {
                    res = queryFlight(xid, number);
                } catch (DeadlockException e) {
                    logger.info(e.toString());
                    deadlock = true;
                }
                sendReply(writer, res, deadlock);
                break;

            case QUERY_FLIGHTS_PRICE:
                xid = request.getInt(XID);
                number = request.getInt(FLIGHT_NUMBER);

                try {
                    res = queryFlightPrice(xid, number);
                } catch (DeadlockException e) {
                    logger.info(e.toString());
                    deadlock = true;
                }
                sendReply(writer, res, deadlock);
                break;

            case RESERVE_FLIGHT:
                xid = request.getInt(XID);
                int customerId = request.getInt(CUSTOMER_ID);
                number = request.getInt(FLIGHT_NUMBER);

                try {
                    result = reserveFlight(xid, customerId, number);
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
        }
    }
}
