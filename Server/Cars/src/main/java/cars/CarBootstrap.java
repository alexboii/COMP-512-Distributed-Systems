package cars;

import Constants.ServerConstants;

public class CarBootstrap {

    public static void main(String[] args) {

        int port = ServerConstants.CAR_SERVER_PORT;

        if (args.length == 1) {
            port = Integer.parseInt(args[0]);
        } else if (args.length != 0 && args.length != 1) {
            System.err.println("Wrong usage");
            System.exit(1);
        }

        CarServer carServer = new CarServer();
        carServer.start(port);

    }
}
