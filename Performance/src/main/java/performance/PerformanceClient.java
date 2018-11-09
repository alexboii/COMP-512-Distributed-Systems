package performance;

import com.github.javafaker.Faker;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

/**
 * Created by alex on 11/7/18.
 */
public class PerformanceClient {
    public static int NUM_CLIENTS = 4;
    public static int LOAD = 100;
    public static int PERIOD = (int) (((1.0 / LOAD) * 1000) * NUM_CLIENTS);
    private static String path = System.getProperty("user.home") + File.separator +
            "group01_data";

    public static void main(String args[]) throws IOException {
//        LOAD = Integer.valueOf(args[2]);
//        NUM_CLIENTS = Integer.valueOf(args[3]);

        Faker faker = new Faker();

        ArrayList<String> fakeCities = new ArrayList();
        ArrayList<Integer> fakeIds = new ArrayList();

        System.out.println("Period" + PERIOD);

        // add at most 10 random cities
        for (int i = 0; i < 20000; i++) {
            fakeCities.add(faker.address().cityName());
        }

        for (int i = 0; i < 20000; i++) {
            fakeIds.add(faker.random().nextInt(10000));
        }

        RandomCommand rc = new RandomCommand(fakeCities, fakeIds);
        BufferedWriter writer = new BufferedWriter(new FileWriter(path));
        CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader("DURATION"));


        ExecutorService es = Executors.newCachedThreadPool();
        for (int i = 0; i < NUM_CLIENTS; i++) {
            ClientThread ct = new ClientThread(PERIOD, rc, LOAD, csvPrinter);
            es.execute(ct);
        }

        es.shutdown();

        try {
            es.awaitTermination(30, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
