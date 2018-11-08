package performance;

import com.github.javafaker.Faker;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by alex on 11/7/18.
 */
public class PerformanceClient {
    public static int NUM_CLIENTS = 1;
    public static int LOAD = 2;
    public static int PERIOD = ((1 / LOAD * 1000) * NUM_CLIENTS);

    public static void main(String args[]) {
//        LOAD = Integer.valueOf(args[2]);
//        NUM_CLIENTS = Integer.valueOf(args[3]);
//        PERIOD = (1 / LOAD * 1000) * NUM_CLIENTS;

        Faker faker = new Faker();

        ArrayList<String> fakeCities = new ArrayList();
        ArrayList<Integer> fakeIds = new ArrayList();

        // add at most 10 random cities
        for (int i = 0; i < 3; i++) {
            fakeCities.add(faker.address().cityName());
        }

        for (int i = 0; i < 3; i++) {
            fakeIds.add(faker.random().nextInt(200));
        }

        RandomCommand rc = new RandomCommand(fakeCities, fakeIds);

        ExecutorService es = Executors.newCachedThreadPool();
        for (int i = 0; i < NUM_CLIENTS; i++) {
            ClientThread ct = new ClientThread(PERIOD, rc, LOAD);
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
