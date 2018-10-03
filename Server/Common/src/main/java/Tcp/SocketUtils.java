package Tcp;

import Constants.ServerConstants;
import RM.IResourceManager;
import RM.ResourceManager;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static Constants.GeneralConstants.RESULT;

public class SocketUtils {

    public static ServerSocket createServerSocket(String host, int port) throws IOException {

        System.out.println("Opening socket at host: " + host + "port: " + port);
        ServerSocket server = new ServerSocket(port, 50, InetAddress.getByName(host));
        System.out.println("Server ready at host: " + host + "port: " + port);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    server.close();
                    System.out.println("Server closed. host: " + host);
                }
                catch(Exception e) {
                    System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
                    e.printStackTrace();
                }
            }
        });
        return server;
    }

    public static void sendReply(OutputStreamWriter writer, int result) throws IOException, JSONException {
        JSONObject reply = new JSONObject();
        reply.put(RESULT, result);
        System.out.println("Sending back reply: " + reply);
        writer.write(reply.toString() + "\n");
        writer.flush();
        return;
    }

    public static void sendReply(OutputStreamWriter writer, String result) throws IOException, JSONException {
        JSONObject reply = new JSONObject();
        reply.put(RESULT, result);
        System.out.println("Sending back reply: " + reply);
        writer.write(reply.toString() + "\n");
        writer.flush();
        return;
    }

    public static void sendReply(OutputStreamWriter writer, boolean result) throws IOException, JSONException {
        JSONObject reply = new JSONObject();
        reply.put(RESULT, result);
        System.out.println("Sending back reply: " + reply);
        writer.write(reply.toString() + "\n");
        writer.flush();
        return;
    }

    public static void sendReply(OutputStreamWriter writer, JSONObject result) throws IOException, JSONException {
        System.out.println("Sending back reply: " + result);
        writer.write(result.toString() + "\n");
        writer.flush();
        return;
    }


    public static JSONObject sendAndReceive(JSONObject request, OutputStreamWriter writer, BufferedReader reader) {
        try {
            writer.write(request.toString() + "\n");
            writer.flush();

            String line = reader.readLine();
            System.out.println("Reply from server: " + line + "\n");
            JSONObject reply = new JSONObject(line);
            return reply;
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static void startServerConnection(String address, int port, int maxConcurrentClients, IResourceManager rm){
        try {
            ServerSocket server = createServerSocket(address, port);

            ExecutorService executors = Executors.newFixedThreadPool(maxConcurrentClients);

            while (true) {

                Socket client = server.accept();
                System.out.println("Got client connection " + client);

                executors.execute(new ProcessRequestRunnable(client, rm));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
