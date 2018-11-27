package TCP;

import RM.IResourceManager;
import Utilities.FileLogger;
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
import java.util.logging.Logger;

import static Constants.GeneralConstants.*;

public class SocketUtils {

    private static final Logger logger = FileLogger.getLogger(SocketUtils.class);

    public static ServerSocket createServerSocket(String host, int port) throws IOException {

        logger.info("Opening socket at host: " + host + "port: " + port);
        ServerSocket server = new ServerSocket(port, 50, InetAddress.getByName(host));
        logger.info("Server ready at host: " + host + "port: " + port);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    server.close();
                    logger.info("Server closed. host: " + host);
                }
                catch(Exception e) {
                    System.err.println((char)27 + "[31;1mServer exception: " + (char)27 + "[0mUncaught exception");
                    e.printStackTrace();
                }
            }
        });
        return server;
    }

    public static <I> void sendReply(OutputStreamWriter writer, I result) throws IOException, JSONException {
        JSONObject reply = new JSONObject();
        reply.put(RESULT, result);
        logger.info("Sending back reply: " + reply);
        writer.write(reply.toString() + "\n");
        writer.flush();
        return;
    }

    public static void sendReply(OutputStreamWriter writer, JSONObject reply) throws IOException, JSONException {
        logger.info("Sending back reply: " + reply);
        writer.write(reply.toString() + "\n");
        writer.flush();
        return;
    }


    public static <I> void sendReply(OutputStreamWriter writer, I result, boolean deadlock) throws IOException, JSONException {
        JSONObject reply = new JSONObject();
        reply.put(RESULT, result);
        if(deadlock) {
            reply.put(DEADLOCK, deadlock);
        }
        logger.info("Sending back reply: " + reply);
        writer.write(reply.toString() + "\n");
        writer.flush();
        return;
    }

    public static void sendReplyToClient(OutputStreamWriter writer, JSONObject result, boolean aborted) throws IOException, JSONException {
        if(aborted){
            result.put(ABORTED, aborted);
        }
        logger.info("Sending back reply to client: " + result);
        writer.write(result.toString() + "\n");
        writer.flush();
        return;
    }

    public static <I> void sendReplyToClient(OutputStreamWriter writer, I result, boolean aborted) throws IOException, JSONException {
        JSONObject reply = new JSONObject();
        reply.put(RESULT, result);
        if(aborted) {
            reply.put(ABORTED, aborted);
        }
        logger.info("Sending back reply to client: " + reply);
        writer.write(reply.toString() + "\n");
        writer.flush();
        return;
    }

    public static void sendRequest(String serverAddress, int port, JSONObject request) {

        try {
            logger.info("Sending request " + request + " to server: " + serverAddress + ":" + port);
            Socket server = new Socket(InetAddress.getByName(serverAddress), port);
            OutputStreamWriter writer = new OutputStreamWriter(server.getOutputStream(), CHAR_SET);

            SocketUtils.send(request, writer);

            server.close();
            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static JSONObject sendAndReceive(JSONObject request, OutputStreamWriter writer, BufferedReader reader) {
        try {
            writer.write(request.toString() + "\n");
            writer.flush();

            String line = reader.readLine();
            logger.info("Reply from server: " + line);
            JSONObject reply = new JSONObject(line);
            return reply;
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static void send(JSONObject request, OutputStreamWriter writer) {
        try {
            writer.write(request.toString() + "\n");
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void startServerConnection(String address, int port, int maxConcurrentClients, IResourceManager rm){
        try {
            ServerSocket server = createServerSocket(address, port);

            ExecutorService executors = Executors.newFixedThreadPool(maxConcurrentClients);

            while (true) {

                Socket client = server.accept();
                logger.info("Got client connection " + client);

                executors.execute(new ProcessRequestRunnable(client, rm));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
