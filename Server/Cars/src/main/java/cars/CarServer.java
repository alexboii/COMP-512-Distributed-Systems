package cars;

import Constants.ServerConstants;
import RM.ResourceManager;
import Tcp.IServer;
import Tcp.SocketUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;

import static Constants.GeneralConstants.*;
import static Tcp.SocketUtils.sendReply;

public class CarServer extends ResourceManager implements IServer {

    private static final String serverName = "Cars";
    private static final int maxConcurrentClients = 10;

    CarServer() {
        super(serverName);
    }

    @Override
    public void start(int port) {
        SocketUtils.startServerConnection(ServerConstants.CAR_SERVER_ADDRESS, port, maxConcurrentClients, this);
    }

    @Override
    public void handleRequest(JSONObject request, OutputStreamWriter writer) throws IOException, JSONException {
        switch ((String) request.get(ACTION)) {

            case ADD_CARS:
                int xid = request.getInt(CAR_XID);
                String location = request.getString(CAR_LOCATION);
                int count = request.getInt(CAR_COUNT);
                int price = request.getInt(CAR_PRICE);

                boolean result = addCars(xid, location, count, price);
                sendReply(writer, result);
                break;

            case DELETE_CARS:
                xid = request.getInt(CAR_XID);
                location = request.getString(CAR_LOCATION);

                result = deleteCars(xid, location);
                sendReply(writer, result);
                break;

            case QUERY_CARS:
                xid = request.getInt(CAR_XID);
                location = request.getString(CAR_LOCATION);

                int res = queryCars(xid, location);
                sendReply(writer, res);
                break;

            case QUERY_CARS_PRICE:
                xid = request.getInt(CAR_XID);
                location = request.getString(CAR_LOCATION);

                res = queryCarsPrice(xid, location);
                sendReply(writer, res);
                break;

            case RESERVE_CARS:
                xid = request.getInt(CAR_XID);
                int customerId = request.getInt(CAR_CUSTOMER_ID);
                location = request.getString(CAR_LOCATION);

                result = reserveCar(xid, customerId, location);
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
