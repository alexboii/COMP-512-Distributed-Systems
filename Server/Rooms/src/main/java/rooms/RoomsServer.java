package rooms;

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
 * Created by alex on 10/2/18.
 */
public class RoomsServer extends ResourceManager implements IServer {
    private static final String serverName = "Rooms";
    private static final int maxConcurrentClients = 10;

    private static final Logger logger = FileLogger.getLogger(RoomsServer.class);

    public RoomsServer() {
        super(serverName);
    }

    @Override
    public void start(int port) {
        SocketUtils.startServerConnection(ServerConstants.ROOMS_SERVER_ADDRESS, port, maxConcurrentClients, this);
    }

    @Override
    public void handleRequest(JSONObject request, OutputStreamWriter writer) throws IOException, JSONException {

        boolean result = false;
        boolean abort = false;
        int res = 0;

        switch ((String) request.get(ACTION)) {

            case ADD_ROOMS:
                int xid = request.getInt(ROOM_XID);
                String location = request.getString(ROOM_LOCATION);
                int count = request.getInt(ROOM_COUNT);
                int price = request.getInt(ROOM_PRICE);

                try {
                    result = addRooms(xid, location, count, price);
                } catch (DeadlockException e) {
                    logger.info(e.toString());
                    abort = true;
                }
                sendReply(writer, result, abort);
                break;

            case DELETE_ROOMS:
                xid = request.getInt(ROOM_XID);
                location = request.getString(ROOM_LOCATION);

                try {
                    result = deleteRooms(xid, location);
                } catch (DeadlockException e) {
                    logger.info(e.toString());
                    abort = true;
                }
                sendReply(writer, result, abort);

                break;

            case QUERY_ROOMS:
                xid = request.getInt(ROOM_XID);
                location = request.getString(ROOM_LOCATION);

                try {
                    res = queryRooms(xid, location);
                } catch (DeadlockException e) {
                    logger.info(e.toString());
                    abort = true;
                }
                sendReply(writer, res, abort);
                break;

            case QUERY_ROOMS_PRICE:
                xid = request.getInt(ROOM_XID);
                location = request.getString(ROOM_LOCATION);

                try {
                    res = queryRoomsPrice(xid, location);
                } catch (DeadlockException e) {
                    logger.info(e.toString());
                    abort = true;
                }
                sendReply(writer, res, abort);
                break;

            case RESERVE_ROOMS:
                xid = request.getInt(ROOM_XID);
                int customerId = request.getInt(CUSTOMER_ID);
                location = request.getString(ROOM_LOCATION);

                try {
                    result = reserveRoom(xid, customerId, location);
                } catch (DeadlockException e) {
                    logger.info(e.toString());
                    abort = true;
                }
                sendReply(writer, result, abort);
                break;

            case NEW_CUSTOMER:
                xid = request.getInt(XID);

                try {
                    res = newCustomer(xid);
                } catch (DeadlockException e) {
                    logger.info(e.toString());
                    abort = true;
                }

                sendReply(writer, res, abort);
                break;

            case NEW_CUSTOMER_ID:
                xid = request.getInt(XID);
                customerId = request.getInt(CUSTOMER_ID);

                try {
                    result = newCustomer(xid, customerId);
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
                    result = deleteCustomer(xid, customerId);
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
