package client;

import Tcp.RequestFactory;
import Tcp.SocketUtils;
import Utilities.InvalidTransactionException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.StringTokenizer;
import java.util.Vector;

import Utilities.TransactionAbortException;

import static Constants.GeneralConstants.ABORTED;
import static Constants.GeneralConstants.RESULT;
import static Constants.GeneralConstants.VALID_XID;

public abstract class Client {
    protected Socket middleware;
    protected OutputStreamWriter middlewareWriter;
    protected BufferedReader middlewareReader;

    public Client() {
        super();
    }

    public abstract void connectServer();

    public void start() {
        // Prepare for reading commands
        System.out.println();
        System.out.println("Location \"Help\" for list of supported commands");

        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));

        while (true) {
            // Read the next command
            String command = "";
            Vector<String> arguments = new Vector<String>();
            try {
                System.out.print((char) 27 + "[32;1m\n>] " + (char) 27 + "[0m");
                command = stdin.readLine().trim();
            } catch (IOException io) {
                System.err.println((char) 27 + "[31;1mClient exception: " + (char) 27 + "[0m" + io.getLocalizedMessage());
                io.printStackTrace();
                System.exit(1);
            }

            try {
                arguments = parse(command);
                Command cmd = Command.fromString((String) arguments.elementAt(0));
                execute(cmd, arguments);
            } catch (IllegalArgumentException e) {
                System.err.println((char) 27 + "[31;1mCommand exception: " + (char) 27 + "[0m" + e.getLocalizedMessage());
            } catch (TransactionAbortException e) {
                System.err.println((char) 27 + "[31;1mCommand exception: " + (char) 27 + "[0m" + e.getLocalizedMessage());
            } catch (InvalidTransactionException e) {
                System.err.println((char) 27 + "[31;1mCommand exception: " + (char) 27 + "[0m" + e.getLocalizedMessage());
            } catch (Exception e) {
                System.err.println((char) 27 + "[31;1mCommand exception: " + (char) 27 + "[0mUncaught exception");
                e.printStackTrace();
            }
        }
    }

    public boolean execute(Command cmd, Vector<String> arguments) throws NumberFormatException, JSONException, TransactionAbortException, InvalidTransactionException {
        boolean success = false;

        switch (cmd) {
            case Help: {
                if (arguments.size() == 1) {
                    System.out.println(Command.description());
                } else if (arguments.size() == 2) {
                    Command l_cmd = Command.fromString((String) arguments.elementAt(1));
                    System.out.println(l_cmd.toString());
                } else {
                    System.err.println((char) 27 + "[31;1mCommand exception: " + (char) 27 + "[0mImproper use of help command. Location \"help\" or \"help,<CommandName>\"");
                }
                success = true;
                break;
            }
            case Start: {
                checkArgumentsCount(1, arguments.size());
                System.out.println("Starting a new transaction");
                JSONObject request = RequestFactory.getNewTransactionRequest();
                JSONObject result = SocketUtils.sendAndReceive(request, middlewareWriter, middlewareReader);
                int xid = result.getInt(RESULT);
                System.out.println("Transaction ID: " + xid);
                success = true;
                break;
            }
            case AddFlight: {
                checkArgumentsCount(5, arguments.size());

                System.out.println("Adding a new flight [xid=" + arguments.elementAt(1) + "]");
                System.out.println("-Flight Number: " + arguments.elementAt(2));
                System.out.println("-Flight Seats: " + arguments.elementAt(3));
                System.out.println("-Flight Price: " + arguments.elementAt(4));

                int id = toInt(arguments.elementAt(1));
                int flightNum = toInt(arguments.elementAt(2));
                int flightSeats = toInt(arguments.elementAt(3));
                int flightPrice = toInt(arguments.elementAt(4));

                JSONObject request = RequestFactory.getAddFlightRequest(id, flightNum, flightSeats, flightPrice);
                JSONObject result = SocketUtils.sendAndReceive(request, middlewareWriter, middlewareReader);

                validate(result);

                if (result.getBoolean(RESULT)) {
                    System.out.println("Flight added");
                    success = true;
                } else {
                    System.out.println("Flight could not be added");
                }

                break;
            }
            case AddCars: {
                checkArgumentsCount(5, arguments.size());

                System.out.println("Adding new cars [xid=" + arguments.elementAt(1) + "]");
                System.out.println("-Car Location: " + arguments.elementAt(2));
                System.out.println("-Number of Cars: " + arguments.elementAt(3));
                System.out.println("-Car Price: " + arguments.elementAt(4));

                int id = toInt(arguments.elementAt(1));
                String location = arguments.elementAt(2);
                int numCars = toInt(arguments.elementAt(3));
                int price = toInt(arguments.elementAt(4));

                JSONObject request = RequestFactory.getAddCarRequest(id, location, numCars, price);
                JSONObject result = SocketUtils.sendAndReceive(request, middlewareWriter, middlewareReader);

                validate(result);

                if (result.getBoolean(RESULT)) {
                    System.out.println("Cars added");
                    success = true;
                } else {
                    System.out.println("Cars could not be added");
                }
                break;
            }
            case AddRooms: {
                checkArgumentsCount(5, arguments.size());

                System.out.println("Adding new rooms [xid=" + arguments.elementAt(1) + "]");
                System.out.println("-Room Location: " + arguments.elementAt(2));
                System.out.println("-Number of Rooms: " + arguments.elementAt(3));
                System.out.println("-Room Price: " + arguments.elementAt(4));

                int id = toInt(arguments.elementAt(1));
                String location = arguments.elementAt(2);
                int numRooms = toInt(arguments.elementAt(3));
                int price = toInt(arguments.elementAt(4));

                JSONObject request = RequestFactory.getAddRoomRequest(id, location, numRooms, price);
                JSONObject result = SocketUtils.sendAndReceive(request, middlewareWriter, middlewareReader);

                validate(result);
                if (result.getBoolean(RESULT)) {
                    System.out.println("Rooms added");
                    success = true;
                } else {
                    System.out.println("Rooms could not be added");
                }
                break;
            }
            case AddCustomer: {
                checkArgumentsCount(2, arguments.size());

                System.out.println("Adding a new customer [xid=" + arguments.elementAt(1) + "]");

                int id = toInt(arguments.elementAt(1));

                JSONObject request = RequestFactory.getAddCustomerRequest(id);
                JSONObject result = SocketUtils.sendAndReceive(request, middlewareWriter, middlewareReader);

                validate(result);
                int customer = result.getInt(RESULT);

                System.out.println("Add customer ID: " + customer);
                success = true;
                break;
            }
            case AddCustomerID: {
                checkArgumentsCount(3, arguments.size());

                System.out.println("Adding a new customer [xid=" + arguments.elementAt(1) + "]");
                System.out.println("-Customer ID: " + arguments.elementAt(2));

                int id = toInt(arguments.elementAt(1));
                int customerID = toInt(arguments.elementAt(2));

                JSONObject request = RequestFactory.getAddCustomerIdRequest(id, customerID);
                JSONObject result = SocketUtils.sendAndReceive(request, middlewareWriter, middlewareReader);

                validate(result);
                if (result.getBoolean(RESULT)) {
                    System.out.println("Add customer ID: " + customerID);
                    success = true;
                } else {
                    System.out.println("Customer could not be added");
                }
                break;
            }
            case DeleteFlight: {
                checkArgumentsCount(3, arguments.size());

                System.out.println("Deleting a flight [xid=" + arguments.elementAt(1) + "]");
                System.out.println("-Flight Number: " + arguments.elementAt(2));

                int id = toInt(arguments.elementAt(1));
                int flightNum = toInt(arguments.elementAt(2));

                JSONObject request = RequestFactory.getDeleteFlightRequest(id, flightNum);
                JSONObject result = SocketUtils.sendAndReceive(request, middlewareWriter, middlewareReader);

                validate(result);
                if (result.getBoolean(RESULT)) {
                    System.out.println("Flight Deleted");
                    success = true;
                } else {
                    System.out.println("Flight could not be deleted");
                }
                break;
            }
            case DeleteCars: {
                checkArgumentsCount(3, arguments.size());

                System.out.println("Deleting all cars at a particular location [xid=" + arguments.elementAt(1) + "]");
                System.out.println("-Car Location: " + arguments.elementAt(2));

                int id = toInt(arguments.elementAt(1));
                String location = arguments.elementAt(2);

                JSONObject request = RequestFactory.getDeleteCarRequest(id, location);
                JSONObject result = SocketUtils.sendAndReceive(request, middlewareWriter, middlewareReader);

                validate(result);
                if (result.getBoolean(RESULT)) {
                    System.out.println("Cars Deleted");
                    success = true;
                } else {
                    System.out.println("Cars could not be deleted");
                }
                break;
            }
            case DeleteRooms: {
                checkArgumentsCount(3, arguments.size());

                System.out.println("Deleting all rooms at a particular location [xid=" + arguments.elementAt(1) + "]");
                System.out.println("-Room Location: " + arguments.elementAt(2));

                int id = toInt(arguments.elementAt(1));
                String location = arguments.elementAt(2);

                JSONObject request = RequestFactory.getDeleteRoomRequest(id, location);
                JSONObject result = SocketUtils.sendAndReceive(request, middlewareWriter, middlewareReader);

                validate(result);
                if (result.getBoolean(RESULT)) {
                    System.out.println("Rooms Deleted");
                    success = true;
                } else {
                    System.out.println("Rooms could not be deleted");
                }
                break;
            }
            case DeleteCustomer: {
                checkArgumentsCount(3, arguments.size());

                System.out.println("Deleting a customer from the database [xid=" + arguments.elementAt(1) + "]");
                System.out.println("-Customer ID: " + arguments.elementAt(2));

                int id = toInt(arguments.elementAt(1));
                int customerID = toInt(arguments.elementAt(2));

                JSONObject request = RequestFactory.getDeleteCustomerRequest(id, customerID);
                JSONObject result = SocketUtils.sendAndReceive(request, middlewareWriter, middlewareReader);

                validate(result);
                if (result.getBoolean(RESULT)) {
                    System.out.println("Customer Deleted");
                    success = true;
                } else {
                    System.out.println("Customer could not be deleted");
                }
                break;
            }
            case QueryFlight: {
                checkArgumentsCount(3, arguments.size());

                System.out.println("Querying a flight [xid=" + arguments.elementAt(1) + "]");
                System.out.println("-Flight Number: " + arguments.elementAt(2));

                int id = toInt(arguments.elementAt(1));
                int flightNum = toInt(arguments.elementAt(2));

                JSONObject request = RequestFactory.getQueryFlightRequest(id, flightNum);
                JSONObject result = SocketUtils.sendAndReceive(request, middlewareWriter, middlewareReader);

                validate(result);
                int seats = result.getInt(RESULT);
                System.out.println("Number of seats available: " + seats);
                success = true;
                break;
            }
            case QueryCars: {
                checkArgumentsCount(3, arguments.size());

                System.out.println("Querying cars location [xid=" + arguments.elementAt(1) + "]");
                System.out.println("-Car Location: " + arguments.elementAt(2));

                int id = toInt(arguments.elementAt(1));
                String location = arguments.elementAt(2);

                JSONObject request = RequestFactory.getQueryCarRequest(id, location);
                JSONObject result = SocketUtils.sendAndReceive(request, middlewareWriter, middlewareReader);

                validate(result);
                int numCars = result.getInt(RESULT);
                System.out.println("Number of cars at this location: " + numCars);
                success = true;
                break;
            }
            case QueryRooms: {
                checkArgumentsCount(3, arguments.size());

                System.out.println("Querying rooms location [xid=" + arguments.elementAt(1) + "]");
                System.out.println("-Room Location: " + arguments.elementAt(2));

                int id = toInt(arguments.elementAt(1));
                String location = arguments.elementAt(2);

                JSONObject request = RequestFactory.getQueryRoomRequest(id, location);
                JSONObject result = SocketUtils.sendAndReceive(request, middlewareWriter, middlewareReader);

                validate(result);
                int numRoom = result.getInt(RESULT);
                System.out.println("Number of rooms at this location: " + numRoom);
                success = true;
                break;
            }
            case QueryCustomer: {
                checkArgumentsCount(3, arguments.size());

                System.out.println("Querying customer information [xid=" + arguments.elementAt(1) + "]");
                System.out.println("-Customer ID: " + arguments.elementAt(2));

                int id = toInt(arguments.elementAt(1));
                int customerID = toInt(arguments.elementAt(2));

                JSONObject request = RequestFactory.getQueryCustomerRequest(id, customerID);
                JSONObject result = SocketUtils.sendAndReceive(request, middlewareWriter, middlewareReader);

                validate(result);
                if(result.has(RESULT)){
                    System.out.print("Bill: " + result.getString(RESULT));
                }
                success = true;
                break;
            }
            case QueryFlightPrice: {
                checkArgumentsCount(3, arguments.size());

                System.out.println("Querying a flight price [xid=" + arguments.elementAt(1) + "]");
                System.out.println("-Flight Number: " + arguments.elementAt(2));

                int id = toInt(arguments.elementAt(1));
                int flightNum = toInt(arguments.elementAt(2));

                JSONObject request = RequestFactory.getQueryFlightPriceRequest(id, flightNum);
                JSONObject result = SocketUtils.sendAndReceive(request, middlewareWriter, middlewareReader);

                validate(result);
                int price = result.getInt(RESULT);
                System.out.println("Price of a seat: " + price);
                success = true;
                break;
            }
            case QueryCarsPrice: {
                checkArgumentsCount(3, arguments.size());

                System.out.println("Querying cars price [xid=" + arguments.elementAt(1) + "]");
                System.out.println("-Car Location: " + arguments.elementAt(2));

                int id = toInt(arguments.elementAt(1));
                String location = arguments.elementAt(2);

                JSONObject request = RequestFactory.getQueryCarPriceRequest(id, location);
                JSONObject result = SocketUtils.sendAndReceive(request, middlewareWriter, middlewareReader);

                validate(result);
                int price = result.getInt(RESULT);
                System.out.println("Price of cars at this location: " + price);
                success = true;
                break;
            }
            case QueryRoomsPrice: {
                checkArgumentsCount(3, arguments.size());

                System.out.println("Querying rooms price [xid=" + arguments.elementAt(1) + "]");
                System.out.println("-Room Location: " + arguments.elementAt(2));

                int id = toInt(arguments.elementAt(1));
                String location = arguments.elementAt(2);

                JSONObject request = RequestFactory.getQueryRoomPriceRequest(id, location);
                JSONObject result = SocketUtils.sendAndReceive(request, middlewareWriter, middlewareReader);

                validate(result);
                int price = result.getInt(RESULT);
                System.out.println("Price of rooms at this location: " + price);
                success = true;
                break;
            }
            case ReserveFlight: {
                checkArgumentsCount(4, arguments.size());

                System.out.println("Reserving seat in a flight [xid=" + arguments.elementAt(1) + "]");
                System.out.println("-Customer ID: " + arguments.elementAt(2));
                System.out.println("-Flight Number: " + arguments.elementAt(3));

                int id = toInt(arguments.elementAt(1));
                int customerID = toInt(arguments.elementAt(2));
                int flightNum = toInt(arguments.elementAt(3));

                JSONObject request = RequestFactory.getReserveFlightRequest(id, customerID, flightNum);
                JSONObject result = SocketUtils.sendAndReceive(request, middlewareWriter, middlewareReader);

                validate(result);
                if (result.getBoolean(RESULT)) {
                    System.out.println("Flight Reserved");
                    success = true;
                } else {
                    System.out.println("Flight could not be reserved");
                }
                break;
            }
            case ReserveCar: {
                checkArgumentsCount(4, arguments.size());

                System.out.println("Reserving a car at a location [xid=" + arguments.elementAt(1) + "]");
                System.out.println("-Customer ID: " + arguments.elementAt(2));
                System.out.println("-Car Location: " + arguments.elementAt(3));

                int id = toInt(arguments.elementAt(1));
                int customerID = toInt(arguments.elementAt(2));
                String location = arguments.elementAt(3);

                JSONObject request = RequestFactory.getReserveCarRequest(id, customerID, location);
                JSONObject result = SocketUtils.sendAndReceive(request, middlewareWriter, middlewareReader);

                validate(result);
                if (result.getBoolean(RESULT)) {
                    System.out.println("Car Reserved");
                    success = true;
                } else {
                    System.out.println("Car could not be reserved");
                }
                break;
            }
            case ReserveRoom: {
                checkArgumentsCount(4, arguments.size());

                System.out.println("Reserving a room at a location [xid=" + arguments.elementAt(1) + "]");
                System.out.println("-Customer ID: " + arguments.elementAt(2));
                System.out.println("-Room Location: " + arguments.elementAt(3));

                int id = toInt(arguments.elementAt(1));
                int customerID = toInt(arguments.elementAt(2));
                String location = arguments.elementAt(3);

                JSONObject request = RequestFactory.getReserveRoomRequest(id, customerID, location);
                JSONObject result = SocketUtils.sendAndReceive(request, middlewareWriter, middlewareReader);

                validate(result);
                if (result.getBoolean(RESULT)) {
                    System.out.println("Room Reserved");
                    success = true;
                } else {
                    System.out.println("Room could not be reserved");
                }
                break;
            }
            case Bundle: {
                if (arguments.size() < 7) {
                    System.err.println((char) 27 + "[31;1mCommand exception: " + (char) 27 + "[0mBundle command expects at least 7 arguments. Location \"Help\" or \"Help,<CommandName>\"");
                    break;
                }
                System.out.println("Reserving an bundle [xid=" + arguments.elementAt(1) + "]");
                System.out.println("-Customer ID: " + arguments.elementAt(2));
                for (int i = 0; i < arguments.size() - 6; ++i) {
                    System.out.println("-Flight Number: " + arguments.elementAt(3 + i));
                }
                System.out.println("-Car Location: " + arguments.elementAt(arguments.size() - 2));
                System.out.println("-Room Location: " + arguments.elementAt(arguments.size() - 1));

                int id = toInt(arguments.elementAt(1));
                int customerID = toInt(arguments.elementAt(2));
                Vector<String> flightNumbers = new Vector<String>();
                for (int i = 0; i < arguments.size() - 6; ++i) {
                    flightNumbers.addElement(arguments.elementAt(3 + i));
                }
                String location = arguments.elementAt(arguments.size() - 3);
                boolean bookCar = toBoolean(arguments.elementAt(arguments.size() - 2));
                boolean bookRoom = toBoolean(arguments.elementAt(arguments.size() - 1));

                JSONObject request = RequestFactory.getBundleRequest(id, customerID, flightNumbers, location, bookCar, bookRoom);
                JSONObject result = SocketUtils.sendAndReceive(request, middlewareWriter, middlewareReader);

                validate(result);
                if (result.getBoolean(RESULT)) {
                    System.out.println("Bundle Reserved");
                    success = true;
                } else {
                    System.out.println("Bundle could not be reserved");
                }
                break;
            }
            case Quit: {
                checkArgumentsCount(1, arguments.size());

                System.out.println("Quitting client");
                System.exit(0);
                break;
            }
            case Commit: {
                checkArgumentsCount(2, arguments.size());
                int xid = toInt(arguments.elementAt(1));
                System.out.println("Committing the transaction [xid=" + xid + "]");

                JSONObject request = RequestFactory.getCommitRequest(xid);
                JSONObject result = SocketUtils.sendAndReceive(request, middlewareWriter, middlewareReader);

                validate(result);
                if (result.getBoolean(RESULT)) {
                    System.out.println("Transaction committed");
                    success = true;
                } else {
                    System.out.println("Unable to commit the transaction. See server logs");
                }
                break;
            }
            case Abort: {
                checkArgumentsCount(2, arguments.size());
                int xid = toInt(arguments.elementAt(1));
                System.out.println("Aborting the transaction [xid=" + xid + "]");

                JSONObject request = RequestFactory.getAbortRequest(xid);
                JSONObject result = SocketUtils.sendAndReceive(request, middlewareWriter, middlewareReader);

                validate(result);
                if (result.getBoolean(RESULT)) {
                    System.out.println("Transaction aborted");
                    success = true;
                } else {
                    System.out.println("Unable to abort the transaction. See server logs");
                }
                break;
            }

        }
        return success;
    }

    private void validate(JSONObject result) throws JSONException, TransactionAbortException, InvalidTransactionException {
        if(result.has(VALID_XID) && !result.getBoolean(VALID_XID)) {
            throw new InvalidTransactionException("No active transactions with the given XID");
        }

        if(result.has(ABORTED) && result.getBoolean(ABORTED)) {
            throw new TransactionAbortException("Transaction aborted. See server logs.");
        }
    }

    public static Vector<String> parse(String command) {
        Vector<String> arguments = new Vector<String>();
        StringTokenizer tokenizer = new StringTokenizer(command, ",");
        String argument = "";
        while (tokenizer.hasMoreTokens()) {
            argument = tokenizer.nextToken();
            argument = argument.trim();
            arguments.add(argument);
        }
        return arguments;
    }

    public static void checkArgumentsCount(Integer expected, Integer actual) throws IllegalArgumentException {
        if (expected != actual) {
            throw new IllegalArgumentException("Invalid number of arguments. Expected " + (expected - 1) + ", received " + (actual - 1) + ". Location \"Help,<CommandName>\" to check usage of this command");
        }
    }

    public static int toInt(String string) throws NumberFormatException {
        return (new Integer(string)).intValue();
    }

    public static boolean toBoolean(String string)// throws Exception
    {
        return (new Boolean(string)).booleanValue();
    }
}
