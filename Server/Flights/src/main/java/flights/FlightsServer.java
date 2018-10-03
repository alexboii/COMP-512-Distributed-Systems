package flights;

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
 * Created by alex on 9/25/18.
 */
public class FlightsServer extends ResourceManager implements IServer {
    private static final String serverName = "Flights";
    private static final int maxConcurrentClients = 10;

    public FlightsServer() {
        super(serverName);
    }

    @Override
    public void start(int port) {
        SocketUtils.startServerConnection(ServerConstants.FLIGHTS_SERVER_NAME, port, maxConcurrentClients, this);
    }

    @Override
    public void handleRequest(JSONObject request, OutputStreamWriter writer) throws IOException, JSONException {
        switch ((String) request.get(ACTION)) {
            case ADD_FLIGHTS:
                int xid = request.getInt(FLIGHT_XID);
                int number = request.getInt(FLIGHT_NUMBER);
                int count = request.getInt(FLIGHT_SEATS);
                int price = request.getInt(FLIGHT_PRICE);

                boolean result = addFlight(xid, number, count, price);
                sendReply(writer, result);
                break;

            case DELETE_FLIGHTS:
                xid = request.getInt(FLIGHT_XID);
                number = request.getInt(FLIGHT_NUMBER);

                result = deleteFlight(xid, number);
                sendReply(writer, result);
                break;

            case QUERY_FLIGHTS:
                xid = request.getInt(FLIGHT_XID);
                number = request.getInt(FLIGHT_NUMBER);

                int res = queryFlight(xid, number);
                sendReply(writer, res);
                break;

            case QUERY_FLIGHTS_PRICE:
                xid = request.getInt(FLIGHT_XID);
                number = request.getInt(FLIGHT_NUMBER);

                res = queryFlightPrice(xid, number);
                sendReply(writer, res);
                break;

            case RESERVE_FLIGHTS:
                xid = request.getInt(FLIGHT_XID);
                int customerId = request.getInt(CUSTOMER_ID);
                number = request.getInt(FLIGHT_NUMBER);

                result = reserveFlight(xid, customerId, number);
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
