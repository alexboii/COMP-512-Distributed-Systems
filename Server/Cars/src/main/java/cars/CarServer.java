package cars;

import Constants.ServerConstants;
import RM.ResourceManager;
import org.json.JSONObject;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static Constants.GeneralConstants.*;
import static Tcp.SocketUtils.createServerSocket;

public class CarServer extends ResourceManager {

    private static final String serverName = "Cars";
    private static final int maxConcurrentClients = 10;

    CarServer() {
        super(serverName);
    }

    void start (int port) {
        try {
            ServerSocket server = createServerSocket(ServerConstants.CAR_SERVER_NAME, port);

            ExecutorService executors = Executors.newFixedThreadPool(maxConcurrentClients);

            while (true) {

                Socket client = server.accept();
                System.out.println("Got client connection " + client);

                executors.execute(new Runnable() {
                    @Override
                    public void run() {
                        BufferedReader reader = null;
                        try{
                            reader = new BufferedReader(new InputStreamReader(client.getInputStream(), "UTF-8"));
                            OutputStreamWriter writer = new OutputStreamWriter(client.getOutputStream(), "UTF-8");

                            String line = null;
                            while ((line = reader.readLine()) != null ) {
                                System.out.println("Received: " + line);
                                handle(new JSONObject(line), writer);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                reader.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handle(JSONObject request, OutputStreamWriter writer) throws IOException {

        switch((String)request.get(ACTION)) {

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
        }

    }

    private void sendReply(OutputStreamWriter writer, boolean result) throws IOException {
        JSONObject reply = new JSONObject();
        reply.put(RESULT, result);
        System.out.println("Sending back reply: " + reply);
        writer.write(reply.toString() + "\n");
        writer.flush();
        return;
    }

    private void sendReply(OutputStreamWriter writer, int result) throws IOException {
        JSONObject reply = new JSONObject();
        reply.put(RESULT, result);
        System.out.println("Sending back reply: " + reply);
        writer.write(reply.toString() + "\n");
        writer.flush();
        return;
    }
}
