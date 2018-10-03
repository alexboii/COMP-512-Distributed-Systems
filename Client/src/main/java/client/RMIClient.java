package client;

import Constants.ServerConstants;
import RM.IResourceManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.RemoteException;
import java.rmi.NotBoundException;

import static Constants.GeneralConstants.CHAR_SET;

public class RMIClient extends Client {
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
            System.err.println((char) 27 + "[31;1mClient exception: " + (char) 27 + "[0mUsage: java client.client.RMIClient [server_hostname [server_rmiobject]]");
            System.exit(1);
        }

        // Set the security policy
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }

        // Get a reference to the RMIRegister
        try {
            RMIClient client = new RMIClient();
            client.connectServer();
            client.start();
        } catch (Exception e) {
            System.err.println((char) 27 + "[31;1mClient exception: " + (char) 27 + "[0mUncaught exception");
            e.printStackTrace();
            System.exit(1);
        }
    }

    public RMIClient() {
        super();
    }

    public void connectServer() {
        connectServer(s_serverHost, s_serverPort, s_serverName);
    }

    public void connectServer(String server, int port, String name) {
        try {
            middleware = new Socket(InetAddress.getByName(server), port);
            System.out.println("Connected to middleware at: " + server + ":" + port);
            middlewareWriter = new OutputStreamWriter(middleware.getOutputStream(), CHAR_SET);
            middlewareReader = new BufferedReader(new InputStreamReader(middleware.getInputStream(), CHAR_SET));

            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    try {
                        middleware.close();
                        middlewareWriter.close();
                        middlewareReader.close();
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
}

