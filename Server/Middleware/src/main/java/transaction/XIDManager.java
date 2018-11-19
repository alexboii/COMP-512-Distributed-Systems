package transaction;

import Persistence.PersistedFile;
import Utilities.FileLogger;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static Constants.GeneralConstants.*;

public class XIDManager {

    private AtomicInteger xid_counter;
    private PersistedFile<Map<Integer, Set>> persistedTransactions;
    private final String COORDINATOR_NAME = "coordinator";

    public Map<Integer, Set> activeTransactions;


    private static final Logger logger = FileLogger.getLogger(XIDManager.class);

    public XIDManager() {
        xid_counter = new AtomicInteger(0);
        activeTransactions = new ConcurrentHashMap<>();
        persistedTransactions = new PersistedFile<>(COORDINATOR_NAME, COORDINATOR_FLAG);

        this.loadData();
    }

    public int newTransaction() {
        int xid = xid_counter.getAndAdd(1);
        logger.info("New xid=" + xid);
        activeTransactions.put(xid, ConcurrentHashMap.newKeySet(4));
        persistData();
        return xid;
    }


    public boolean validate(JSONObject request) throws JSONException {
        if (request.get(TYPE).equals(TRANSACTION) && request.get(ACTION).equals(NEW_TRANSACTION)) {
            return true;
        }
        if (request.get(TYPE).equals(OTHERS) && request.get(ACTION).equals(SHUTDOWN)) {
            return true;
        }
        if (activeTransactions.containsKey(request.getInt(XID))) {
            return true;
        }
        logger.severe("validation failed");
        return false;
    }

    public Set<String> completeTransaction(int xid) {
        logger.info("Completing transaction xid=" + xid);
        Set rms = activeTransactions.remove(xid);
        logger.info("RMs involved " + rms);
        persistData();
        return rms;

    }

    public Map<Integer, Set> getActiveTransactions() {
        return activeTransactions;
    }

    public void addRM(int xid, String RM) {
        activeTransactions.get(xid).add(RM);
        persistData();
    }

    private void loadData() {
        try {
            logger.info("Loading data for " + COORDINATOR_NAME);
            this.activeTransactions = this.persistedTransactions.read();
        } catch (IOException | ClassNotFoundException e) {
            logger.info("Unable to load data for " + COORDINATOR_NAME);
            e.printStackTrace();
        }
    }

    public void persistData() {
        try {
            this.persistedTransactions.save(this.activeTransactions);
            logger.info("Successfully saved committed data for " + COORDINATOR_NAME);
        } catch (IOException e) {
            e.printStackTrace();
            logger.info("Unable to write committed data to disk for " + COORDINATOR_NAME);
        }
    }


}
