package Tcp;

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

import static Constants.GeneralConstants.SHOULD_ABORT;
import static Constants.GeneralConstants.RESULT;

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

    public static <I> void sendReply(OutputStreamWriter writer, I result, boolean abort) throws IOException, JSONException {
        JSONObject reply = new JSONObject();
        reply.put(RESULT, result);
        reply.put(SHOULD_ABORT, abort);
        logger.info("Sending back reply: " + reply);
        writer.write(reply.toString() + "\n");
        writer.flush();
        return;
    }

    public static void sendReply(OutputStreamWriter writer, JSONObject result) throws IOException, JSONException {
        logger.info("Sending back reply: " + result);
        writer.write(result.toString() + "\n");
        writer.flush();
        return;
    }


    public static JSONObject sendAndReceive(JSONObject request, OutputStreamWriter writer, BufferedReader reader) {
        System.out.println(request);

        try {
            System.out.println(request.toString() );

            writer.write(request.toString() + "\n");

            writer.flush();

            String line = reader.readLine();
            logger.info("Reply from server: " + line + "\n");
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
                logger.info("Got client connection " + client);

                executors.execute(new ProcessRequestRunnable(client, rm));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
