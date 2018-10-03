package rooms;

import Constants.ServerConstants;
import RM.ResourceManager;
import Tcp.IServer;
import Tcp.SocketUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStreamWriter;


import static Constants.GeneralConstants.*;
import static Tcp.SocketUtils.sendReply;

/**
 * Created by alex on 10/2/18.
 */
public class RoomsServer extends ResourceManager implements IServer {
    private static final String serverName = "Rooms";
    private static final int maxConcurrentClients = 10;

    public RoomsServer() {
        super(serverName);
    }
    
    @Override
    public void start(int port) {
        SocketUtils.startServerConnection(ServerConstants.ROOMS_SERVER_ADDRESS, port, maxConcurrentClients, this);
    }

    @Override
    public void handleRequest(JSONObject request, OutputStreamWriter writer) throws IOException, JSONException {
        switch ((String) request.get(ACTION)) {

            case ADD_ROOMS:
                int xid = request.getInt(ROOM_XID);
                String location = request.getString(ROOM_LOCATION);
                int count = request.getInt(ROOM_COUNT);
                int price = request.getInt(ROOM_PRICE);

                boolean result = addRooms(xid, location, count, price);
                sendReply(writer, result);
                break;

            case DELETE_ROOMS:
                xid = request.getInt(ROOM_XID);
                location = request.getString(ROOM_LOCATION);

                result = deleteRooms(xid, location);
                sendReply(writer, result);
                break;

            case QUERY_ROOMS:
                xid = request.getInt(ROOM_XID);
                location = request.getString(ROOM_LOCATION);

                int res = queryRooms(xid, location);
                sendReply(writer, res);
                break;

            case QUERY_ROOMS_PRICE:
                xid = request.getInt(ROOM_XID);
                location = request.getString(ROOM_LOCATION);

                res = queryRoomsPrice(xid, location);
                sendReply(writer, res);
                break;

            case RESERVE_ROOMS:
                xid = request.getInt(ROOM_XID);
                int customerId = request.getInt(CUSTOMER_ID);
                location = request.getString(ROOM_LOCATION);

                result = reserveRoom(xid, customerId, location);
                sendReply(writer, result);
                break;

            case NEW_CUSTOMER:
                xid = request.getInt(CUSTOMER_XID);

                res = newCustomer(xid);

                sendReply(writer, res);
                break;

            case NEW_CUSTOMER_ID:
                xid = request.getInt(CUSTOMER_XID);
                customerId = request.getInt(CUSTOMER_ID);

                result = newCustomer(xid, customerId);

                sendReply(writer, result);
                break;

            case DELETE_CUSTOMER:
                xid = request.getInt(CUSTOMER_XID);
                customerId = request.getInt(CUSTOMER_ID);
                result = deleteCustomer(xid, customerId);

                sendReply(writer, result);
                break;
        }
    }
}
