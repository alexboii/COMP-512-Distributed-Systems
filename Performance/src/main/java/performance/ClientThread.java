package performance;

import client.Command;
import client.TCPClient;
import org.json.JSONException;

import java.security.SecureRandom;
import java.util.Vector;

/**
 * Created by alex on 11/7/18.
 */
public class ClientThread implements Runnable {
    private static final SecureRandom random = new SecureRandom();

    private TCPClient client;
    private int period;
    private RandomCommand rc;
    private int load;

    public ClientThread(int period, RandomCommand rc, int load) {
        this.client = new TCPClient();

        this.period = period;
        this.rc = rc;
        this.load = load;
    }

    @Override
    public void run() {
        this.client.connectServer();

        for (int i = 0; i < load; i++) {
            long start = System.currentTimeMillis();
            try {
                int xid = this.client.startTransaction();
                Vector<String> vector = new Vector<>();
                vector.add(String.valueOf(xid));
                boolean aborted = false;


                for (int j = 0; j < 20; j++) {
                    if (rc.executeRandomCommand(xid, this.client)) {
                        vector.add(0, Command.Abort.toString());
                        this.client.execute(Command.Abort, vector);
                        aborted = true;

                        break;
                    }
                }

                long duration = System.currentTimeMillis() - start;
                System.out.println("DURATION" + duration);

                if (!aborted) {
                    vector.add(0, Command.Commit.toString());
                    this.client.execute(Command.Commit, vector);
                }

                // introduce uniform variation
                long wait = (this.period + random.nextInt(5 - (-5)) - 5) - duration;

                if (wait > 0) {
                    try {
                        Thread.sleep(wait);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
