package Tcp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;

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
}
