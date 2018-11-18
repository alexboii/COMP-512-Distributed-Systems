package rooms;

import Constants.ServerConstants;

/**
 * Created by alex on 10/2/18.
 */
public class RoomsBootstrap {
    public static void main(String[] args) {

        int port = ServerConstants.ROOMS_SERVER_PORT;

        if (args.length == 1) {
            port = Integer.parseInt(args[0]);
        } else if (args.length != 0 && args.length != 1) {
            System.err.println("Wrong usage");
            System.exit(1);
        }

        RoomsServer roomsServer = new RoomsServer();
        roomsServer.start(port);
    }
}
