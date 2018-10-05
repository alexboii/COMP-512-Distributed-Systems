import client.Command;
import client.TCPClient;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.Vector;

import static client.Client.parse;
import static org.hamcrest.CoreMatchers.containsString;

@Ignore
public class TestClient {

    @Test
    public void flightReservationTest() throws JSONException, IOException {

        TCPClient testClient = new TCPClient();
        testClient.connectServer();

        //add flight
        String command = "AddFlight,1,2200,100,950";
        boolean result = execute(command, testClient);
        Assert.assertTrue(result);

        LoggedPrintStream lpsOut = LoggedPrintStream.create(System.out);
        System.setOut(lpsOut);
        command = "QueryFlight,1,2200";
        result = execute(command, testClient);
        Assert.assertTrue(result);
        System.setOut(lpsOut.underlying);
        Assert.assertThat(lpsOut.buf.toString(), containsString("Number of seats available: 100"));

        command = "AddCustomerID,23,90";
        execute(command, testClient);

        //make reservtion
        command = "ReserveFlight,1,90,2200";
        result = execute(command, testClient);
        Assert.assertTrue(result);

        //verify reservation successful
        System.setOut(lpsOut);
        command = "QueryCustomer,23,90";
        result = execute(command, testClient);
        Assert.assertTrue(result);
        System.setOut(lpsOut.underlying);
        Assert.assertThat(lpsOut.buf.toString(), containsString("$950"));

        System.setOut(lpsOut);
        command = "QueryFlight,1,2200";
        result = execute(command, testClient);
        Assert.assertTrue(result);
        System.setOut(lpsOut.underlying);
        Assert.assertThat(lpsOut.buf.toString(), containsString("Number of seats available: 99"));

        //clean up TODO: delete reservation
        execute("DeleteCustomer,65,234", testClient);
        execute("DeleteFlight,1,2200", testClient);

        testClient.destroyConnection();
    }

    @Test
    public void carReservationTest() throws JSONException, IOException {

        TCPClient testClient = new TCPClient();
        testClient.connectServer();

        String command = "AddCars,223,plas,5,100";
        boolean result = execute(command, testClient);
        Assert.assertTrue(result);

        LoggedPrintStream lpsOut = LoggedPrintStream.create(System.out);
        System.setOut(lpsOut);
        command = "QueryCars,223,plas";
        result = execute(command, testClient);
        Assert.assertTrue(result);
        System.setOut(lpsOut.underlying);
        Assert.assertThat(lpsOut.buf.toString(), containsString("Number of cars at this location: 5"));

        command = "AddCustomerID,33,45";
        execute(command, testClient);

        command = "ReserveCar,223,45,plas";
        result = execute(command, testClient);
        Assert.assertTrue(result);

        System.setOut(lpsOut);
        command = "QueryCustomer,33,45";
        result = execute(command, testClient);
        Assert.assertTrue(result);
        System.setOut(lpsOut.underlying);
        Assert.assertThat(lpsOut.buf.toString(), containsString("$100"));

        System.setOut(lpsOut);
        command = "QueryCars,223,plas";
        result = execute(command, testClient);
        Assert.assertTrue(result);
        System.setOut(lpsOut.underlying);
        Assert.assertThat(lpsOut.buf.toString(), containsString("Number of cars at this location: 4"));

        //clean up TODO: delete reservation
        execute("DeleteCustomer,65,234", testClient);
        execute("DeleteCars,223,plas", testClient);

        testClient.destroyConnection();
    }

    @Test
    public void roomReservationTest() throws JSONException, IOException {

        TCPClient testClient = new TCPClient();
        testClient.connectServer();

        String command = "AddRooms,65,chicago,25,130";
        boolean result = execute(command, testClient);
        Assert.assertTrue(result);

        LoggedPrintStream lpsOut = LoggedPrintStream.create(System.out);
        System.setOut(lpsOut);
        command = "QueryRoomsPrice,65,chicago";
        result = execute(command, testClient);
        Assert.assertTrue(result);
        System.setOut(lpsOut.underlying);
        Assert.assertThat(lpsOut.buf.toString(), containsString("Price of rooms at this location: 130"));

        command = "AddCustomerID,87564,234";
        execute(command, testClient);

        command = "ReserveRoom,65,234,chicago";
        result = execute(command, testClient);
        Assert.assertTrue(result);

        System.setOut(lpsOut);
        command = "QueryRooms,65,chicago";
        result = execute(command, testClient);
        Assert.assertTrue(result);
        System.setOut(lpsOut.underlying);
        Assert.assertThat(lpsOut.buf.toString(), containsString("Number of rooms at this location: 24"));

        System.setOut(lpsOut);
        command = "QueryCustomer,87564,234";
        result = execute(command, testClient);
        Assert.assertTrue(result);
        System.setOut(lpsOut.underlying);
        Assert.assertThat(lpsOut.buf.toString(), containsString("$130"));

        //clean up TODO: delete reservation
        execute("DeleteCustomer,65,234", testClient);
        execute("DeleteRooms,65,chicago", testClient);

        testClient.destroyConnection();
    }

    @Test
    public void bundleReservationTest() throws JSONException, IOException {

        TCPClient testClient = new TCPClient();
        testClient.connectServer();

        //add two flights, rooms and cars
        String command = "AddFlight,1,2200,100,950";
        boolean result = execute(command, testClient);
        Assert.assertTrue(result);

        command = "AddFlight,2,2300,100,850";
        result = execute(command, testClient);
        Assert.assertTrue(result);

        command = "AddCars,223,ny,5,100";
        result = execute(command, testClient);
        Assert.assertTrue(result);

        command = "AddRooms,65,ny,25,130";
        result = execute(command, testClient);
        Assert.assertTrue(result);

        //add customer
        command = "AddCustomerID,213434,22";
        execute(command, testClient);


        //make reservation in bundle
        command = "Bundle,213434,22,2200,2300,ny,true,true";
        result = execute(command, testClient);
        Assert.assertTrue(result);

        LoggedPrintStream lpsOut = LoggedPrintStream.create(System.out);
        System.setOut(lpsOut);
        command = "QueryCustomer,213434,22";
        result = execute(command, testClient);
        Assert.assertTrue(result);
        System.setOut(lpsOut.underlying);
        Assert.assertThat(lpsOut.buf.toString(), containsString("$950"));
        Assert.assertThat(lpsOut.buf.toString(), containsString("$850"));
        Assert.assertThat(lpsOut.buf.toString(), containsString("$130"));
        Assert.assertThat(lpsOut.buf.toString(), containsString("$100"));

        //verify reservation successfully made

        System.setOut(lpsOut);
        command = "QueryFlight,1,2200";
        result = execute(command, testClient);
        Assert.assertTrue(result);
        System.setOut(lpsOut.underlying);
        Assert.assertThat(lpsOut.buf.toString(), containsString("Number of seats available: 99"));

        System.setOut(lpsOut);
        command = "QueryCars,223,ny";
        result = execute(command, testClient);
        Assert.assertTrue(result);
        System.setOut(lpsOut.underlying);
        Assert.assertThat(lpsOut.buf.toString(), containsString("Number of cars at this location: 4"));

        System.setOut(lpsOut);
        command = "QueryRooms,65,ny";
        result = execute(command, testClient);
        Assert.assertTrue(result);
        System.setOut(lpsOut.underlying);
        Assert.assertThat(lpsOut.buf.toString(), containsString("Number of rooms at this location: 24"));


        //clean up TODO: delete reservation
        execute("DeleteCustomer,213434,22", testClient);
        execute("DeleteFlight,1,2200", testClient);
        execute("DeleteFlight,2,2300", testClient);
        execute("DeleteCars,223,ny", testClient);
        execute("DeleteRooms,65,ny", testClient);

        testClient.destroyConnection();
    }

    @Test
    public void helpCommandTest() throws JSONException, IOException {
        TCPClient testClient = new TCPClient();
        testClient.connectServer();
        String command = "Help";

        boolean result = execute(command, testClient);
        Assert.assertTrue(result);
        testClient.destroyConnection();
    }

    private boolean execute(String command, TCPClient testClient) throws JSONException {
        Vector<String> arguments = parse(command);
        Command cmd = Command.fromString(arguments.elementAt(0));

        return testClient.execute(cmd, arguments);
    }
}
