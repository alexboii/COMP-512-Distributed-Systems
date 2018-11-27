package Utilities;

import static Constants.GeneralConstants.CAR_ENTITY;
import static Constants.GeneralConstants.FLIGHT_ENTITY;
import static Constants.GeneralConstants.ROOM_ENTITY;
import static Constants.ServerConstants.*;

public class RMNameServerUtil {

    public static String nameToHost(String rm) {
        if(rm.equals(CAR_ENTITY)){
            return CAR_SERVER_ADDRESS;
        }
        if(rm.equals(FLIGHT_ENTITY)){
            return FLIGHTS_SERVER_ADDRESS;
        }
        if(rm.equals(ROOM_ENTITY)){
            return ROOMS_SERVER_ADDRESS;
        }
        return null;
    }

    public static int nameToPort(String rm) {
        if(rm.equals(CAR_ENTITY)){
            return CAR_SERVER_PORT;
        }
        if(rm.equals(FLIGHT_ENTITY)){
            return FLIGHTS_SERVER_PORT;
        }
        if(rm.equals(ROOM_ENTITY)){
            return ROOMS_SERVER_PORT;
        }
        return 0;
    }
}
