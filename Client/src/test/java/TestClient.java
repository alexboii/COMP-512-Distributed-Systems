import Utilities.InvalidTransactionException;
import Utilities.TransactionAbortException;
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

    //TODO: Get XIDs from output of Start

    @Test
    public void flightReservationTest() throws JSONException, IOException, InvalidTransactionException {

        TCPClient testClient = new TCPClient();
        testClient.connectServer();

        try {
            execute("Start", testClient);

            //add flight
            String command = "AddFlight,0,22001,100,950";
            boolean result = execute(command, testClient);
            Assert.assertTrue(result);

            LoggedPrintStream lpsOut = LoggedPrintStream.create(System.out);
            System.setOut(lpsOut);
            command = "QueryFlight,0,22001";
            result = execute(command, testClient);
            Assert.assertTrue(result);
            System.setOut(lpsOut.underlying);
            Assert.assertThat(lpsOut.buf.toString(), containsString("Number of seats available: 100"));

            command = "AddCustomerID,0,90";
            execute(command, testClient);

            //make reservation
            command = "ReserveFlight,0,90,22001";
            result = execute(command, testClient);
            Assert.assertTrue(result);

            //verify reservation successful
            System.setOut(lpsOut);
            command = "QueryCustomer,0,90";
            result = execute(command, testClient);
            Assert.assertTrue(result);
            System.setOut(lpsOut.underlying);
            Assert.assertThat(lpsOut.buf.toString(), containsString("$950"));

            System.setOut(lpsOut);
            command = "QueryFlight,0,22001";
            result = execute(command, testClient);
            Assert.assertTrue(result);
            System.setOut(lpsOut.underlying);
            Assert.assertThat(lpsOut.buf.toString(), containsString("Number of seats available: 99"));

            //clean up TODO: delete reservation
            //execute("DeleteCustomer,1,234", testClient);
            //execute("DeleteFlight,1,2200", testClient);
            execute("Commit,0", testClient);
        } catch (TransactionAbortException e) {
            e.printStackTrace();
        }
        testClient.destroyConnection();
    }

    @Test
    public void carReservationTest() throws JSONException, IOException, InvalidTransactionException {

        TCPClient testClient = new TCPClient();
        testClient.connectServer();

        try{
            execute("Start", testClient);

            String command = "AddCars,1,plas,5,100";
            boolean result = execute(command, testClient);
            Assert.assertTrue(result);

            LoggedPrintStream lpsOut = LoggedPrintStream.create(System.out);
            System.setOut(lpsOut);
            command = "QueryCars,1,plas";
            result = execute(command, testClient);
            Assert.assertTrue(result);
            System.setOut(lpsOut.underlying);
            Assert.assertThat(lpsOut.buf.toString(), containsString("Number of cars at this location: 5"));

            command = "AddCustomerID,1,45";
            execute(command, testClient);

            command = "ReserveCar,1,45,plas";
            result = execute(command, testClient);
            Assert.assertTrue(result);

            System.setOut(lpsOut);
            command = "QueryCustomer,1,45";
            result = execute(command, testClient);
            Assert.assertTrue(result);
            System.setOut(lpsOut.underlying);
            Assert.assertThat(lpsOut.buf.toString(), containsString("$100"));

            System.setOut(lpsOut);
            command = "QueryCars,1,plas";
            result = execute(command, testClient);
            Assert.assertTrue(result);
            System.setOut(lpsOut.underlying);
            Assert.assertThat(lpsOut.buf.toString(), containsString("Number of cars at this location: 4"));

            //clean up TODO: delete reservation
            //execute("DeleteCustomer,223,234", testClient);
            //execute("DeleteCars,223,plas", testClient);
            execute("Commit,1", testClient);
        } catch (TransactionAbortException e) {
            e.printStackTrace();
        }

        testClient.destroyConnection();
    }

    @Test
    public void roomReservationTest() throws JSONException, IOException, InvalidTransactionException {

        TCPClient testClient = new TCPClient();
        testClient.connectServer();

        try {
            execute("Start", testClient);

            String command = "AddRooms,2,chicago,25,130";
            boolean result = execute(command, testClient);
            Assert.assertTrue(result);

            LoggedPrintStream lpsOut = LoggedPrintStream.create(System.out);
            System.setOut(lpsOut);
            command = "QueryRoomsPrice,2,chicago";
            result = execute(command, testClient);
            Assert.assertTrue(result);
            System.setOut(lpsOut.underlying);
            Assert.assertThat(lpsOut.buf.toString(), containsString("Price of rooms at this location: 130"));

            command = "AddCustomerID,2,234";
            execute(command, testClient);

            command = "ReserveRoom,2,234,chicago";
            result = execute(command, testClient);
            Assert.assertTrue(result);

            System.setOut(lpsOut);
            command = "QueryRooms,2,chicago";
            result = execute(command, testClient);
            Assert.assertTrue(result);
            System.setOut(lpsOut.underlying);
            Assert.assertThat(lpsOut.buf.toString(), containsString("Number of rooms at this location: 24"));

            System.setOut(lpsOut);
            command = "QueryCustomer,2,234";
            result = execute(command, testClient);
            Assert.assertTrue(result);
            System.setOut(lpsOut.underlying);
            Assert.assertThat(lpsOut.buf.toString(), containsString("$130"));

            //clean up TODO: delete reservation
            //execute("DeleteCustomer,65,234", testClient);
            //execute("DeleteRooms,65,chicago", testClient);
            execute("Abort,2", testClient);

        } catch (TransactionAbortException e) {
            e.printStackTrace();
        }
        testClient.destroyConnection();
    }

    @Test
    public void bundleReservationTest() throws JSONException, IOException, InvalidTransactionException {

        TCPClient testClient = new TCPClient();
        testClient.connectServer();

        try{
            execute("Start", testClient);

            //add two flights, rooms and cars
            String command = "AddFlight,3,2200,100,950";
            boolean result = execute(command, testClient);
            Assert.assertTrue(result);

            command = "AddFlight,3,2300,100,850";
            result = execute(command, testClient);
            Assert.assertTrue(result);

            command = "AddCars,3,ny,5,100";
            result = execute(command, testClient);
            Assert.assertTrue(result);

            command = "AddRooms,3,ny,25,130";
            result = execute(command, testClient);
            Assert.assertTrue(result);

            //add customer
            command = "AddCustomerID,3,22";
            execute(command, testClient);


            //make reservation in bundle
            command = "Bundle,3,22,2200,2300,ny,true,true";
            result = execute(command, testClient);
            Assert.assertTrue(result);

            LoggedPrintStream lpsOut = LoggedPrintStream.create(System.out);
            System.setOut(lpsOut);
            command = "QueryCustomer,3,22";
            result = execute(command, testClient);
            Assert.assertTrue(result);
            System.setOut(lpsOut.underlying);
            Assert.assertThat(lpsOut.buf.toString(), containsString("$950"));
            Assert.assertThat(lpsOut.buf.toString(), containsString("$850"));
            Assert.assertThat(lpsOut.buf.toString(), containsString("$130"));
            Assert.assertThat(lpsOut.buf.toString(), containsString("$100"));

            //verify reservation successfully made

            System.setOut(lpsOut);
            command = "QueryFlight,3,2200";
            result = execute(command, testClient);
            Assert.assertTrue(result);
            System.setOut(lpsOut.underlying);
            Assert.assertThat(lpsOut.buf.toString(), containsString("Number of seats available: 99"));

            System.setOut(lpsOut);
            command = "QueryCars,3,ny";
            result = execute(command, testClient);
            Assert.assertTrue(result);
            System.setOut(lpsOut.underlying);
            Assert.assertThat(lpsOut.buf.toString(), containsString("Number of cars at this location: 4"));

            System.setOut(lpsOut);
            command = "QueryRooms,3,ny";
            result = execute(command, testClient);
            Assert.assertTrue(result);
            System.setOut(lpsOut.underlying);
            Assert.assertThat(lpsOut.buf.toString(), containsString("Number of rooms at this location: 24"));


            //clean up TODO: delete reservation
            //execute("DeleteCustomer,2,22", testClient);
            //execute("DeleteFlight,2,2200", testClient);
            //execute("DeleteFlight,2,2300", testClient);
            //execute("DeleteCars,2,ny", testClient);
            //execute("DeleteRooms,2,ny", testClient);
            execute("Commit,3", testClient);

        } catch (TransactionAbortException e) {
            e.printStackTrace();
        }
        testClient.destroyConnection();
    }

    @Test
    public void helpCommandTest() throws JSONException, IOException {
        TCPClient testClient = new TCPClient();
        testClient.connectServer();
        try {
            execute("Start", testClient);

            String command = "Help";

            boolean result = execute(command, testClient);
            Assert.assertTrue(result);

            execute("Commit,4", testClient);
        } catch (TransactionAbortException e) {
            e.printStackTrace();
        } catch (InvalidTransactionException e) {
            e.printStackTrace();
        }

        testClient.destroyConnection();
    }

    private boolean execute(String command, TCPClient testClient) throws JSONException, TransactionAbortException, InvalidTransactionException {
        Vector<String> arguments = parse(command);
        Command cmd = Command.fromString(arguments.elementAt(0));

        return testClient.execute(cmd, arguments);
    }
}
