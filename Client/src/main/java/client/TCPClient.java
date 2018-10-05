package client;

import Constants.ServerConstants;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;

import static Constants.GeneralConstants.CHAR_SET;

public class TCPClient extends Client {
    private static String s_serverHost = ServerConstants.MIDDLEWARE_SERVER_ADDRESS;
    private static int s_serverPort = ServerConstants.MIDDLEWARE_PORT;
    private static String s_serverName = ServerConstants.MIDDLEWARE_PREFIX;

    //TODO: REPLACE 'ALEX' WITH YOUR GROUP NUMBER TO COMPILE
    private static String s_rmiPrefix = "group01";

    public static void main(String args[]) {
        if (args.length > 0) {
            s_serverHost = args[0];
        }
        if (args.length > 1) {
            s_serverName = args[1];
        }
        if (args.length > 2) {
            System.err.println((char) 27 + "[31;1mClient exception: " + (char) 27 + "[0mUsage: java client.client.TCPClient [server_hostname [server_rmiobject]]");
            System.exit(1);
        }

        // Get a reference to the RMIRegister
        try {
            TCPClient client = new TCPClient();
            client.connectServer();
            client.start();
        } catch (Exception e) {
            System.err.println((char) 27 + "[31;1mClient exception: " + (char) 27 + "[0mUncaught exception");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public TCPClient() {
        super();
    }

    public void connectServer() {
        connectServer(s_serverHost, s_serverPort);
    }

    public void connectServer(String server, int port) {
        try {
            middleware = new Socket(InetAddress.getByName(server), port);
            System.out.println("Connected to middleware at: " + server + ":" + port);
            middlewareWriter = new OutputStreamWriter(middleware.getOutputStream(), CHAR_SET);
            middlewareReader = new BufferedReader(new InputStreamReader(middleware.getInputStream(), CHAR_SET));

            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    try {
                        destroyConnection();
                    } catch (Exception e) {
                        System.err.println((char) 27 + "[31;1mServer exception: " + (char) 27 + "[0mUncaught exception");
                        e.printStackTrace();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void destroyConnection() throws IOException {
        middleware.close();
        middlewareWriter.close();
        middlewareReader.close();
    }
}

