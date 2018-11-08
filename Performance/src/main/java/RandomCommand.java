import Tcp.RequestFactory;
import Tcp.SocketUtils;
import client.Command;
import client.TCPClient;
import com.github.javafaker.Faker;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Vector;

import static Constants.GeneralConstants.RESULT;
import static Constants.GeneralConstants.SHOULD_ABORT;
import static client.Client.parse;

/**
 * Created by alex on 11/7/18.
 */
public class RandomCommand {
    private static final SecureRandom random = new SecureRandom();
    private ArrayList<String> fakeCities;
    private ArrayList<Integer> fakeIds;
    private Faker faker;

    private static String[] commands = {
            "AddFlight",
            "AddCars",
            "AddRooms",
            "AddCustomerID",
            "DeleteFlight",
            "DeleteCars",
            "DeleteRooms",
            "DeleteCustomer",
            "QueryFlight",
            "QueryCars",
            "QueryRooms",
            "QueryCustomer",
            "QueryFlightPrice",
            "QueryCarsPrice",
            "QueryRoomsPrice",
            "ReserveFlight",
            "ReserveCar",
            "ReserveRoom",
            "Bundle",
    };

    public RandomCommand(ArrayList<String> fakeCities, ArrayList<Integer> fakeIds) {
        this.fakeCities = fakeCities;
        this.fakeIds = fakeIds;
        this.faker = new Faker();
    }

    public JSONObject execute(String command, TCPClient testClient) throws JSONException {
        Vector<String> arguments = parse(command);
        Command cmd = Command.fromString(arguments.elementAt(0));

        return testClient.execute(cmd, arguments);
    }

    public boolean executeRandomCommand(int xid, TCPClient client) throws JSONException {
        String command = randomCommand();

        switch (command) {
            case "AddFlight": {
                command = command + "," + xid + "," + randomId() + "," + faker.random().nextInt(50, 200) + "," + faker.random().nextInt(50, 200);
                break;
            }
            case "AddCars": {
                command = command + "," + xid + "," + randomCity() + "," + faker.random().nextInt(50, 200) + "," + faker.random().nextInt(50, 200);
                break;
            }
            case "AddRooms": {
                command = command + "," + xid + "," + randomCity() + "," + faker.random().nextInt(50, 200) + "," + faker.random().nextInt(50, 200);
                break;
            }
            case "AddCustomerID": {
                command = command + "," + xid + "," + randomId();
                break;
            }
            case "DeleteFlight": {
                command = command + "," + xid + "," + randomId();
                break;
            }
            case "DeleteCars": {
                command = command + "," + xid + "," + randomCity();
                break;
            }
            case "DeleteRooms": {
                command = command + "," + xid + "," + randomCity();
                break;
            }
            case "DeleteCustomer": {
                command = command + "," + xid + "," + randomId();
                break;
            }
            case "QueryFlight": {
                command = command + "," + xid + "," + randomId();
                break;
            }
            case "QueryCars": {
                command = command + "," + xid + "," + randomCity();
                break;
            }
            case "QueryRooms": {
                command = command + "," + xid + "," + randomCity();
                break;
            }
            case "QueryCustomer": {
                command = command + "," + xid + "," + randomId();
                break;
            }
            case "QueryFlightPrice": {
                command = command + "," + xid + "," + randomId();
                break;
            }
            case "QueryCarsPrice": {
                command = command + "," + xid + "," + randomCity();
                break;
            }
            case "QueryRoomsPrice": {
                command = command + "," + xid + "," + randomCity();
                break;
            }
            case "ReserveFlight": {
                command = command + "," + xid + "," + randomId() + "," + randomId();
                break;
            }
            case "ReserveCar": {
                command = command + "," + xid + "," + randomId() + "," + randomCity();
                break;
            }
            case "ReserveRoom": {
                command = command + "," + xid + "," + randomId() + "," + randomCity();
                break;
            }
        }

        JSONObject result = execute(command, client);

        return result.has(SHOULD_ABORT) && result.getBoolean(SHOULD_ABORT);
    }

    private String randomCommand() {
        int i = random.nextInt(commands.length);
        return commands[i];
    }

    private String randomId() {
        int i = random.nextInt(fakeIds.size());
        return String.valueOf(fakeIds.get(i));
    }

    private String randomCity() {
        int i = random.nextInt(fakeCities.size());
        return fakeCities.get(i);
    }
}
