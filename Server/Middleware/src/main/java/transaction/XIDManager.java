package transaction;

import Utilities.FileLogger;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static Constants.GeneralConstants.*;

public class XIDManager {

    private AtomicInteger xid_counter;
    private Map<Integer, Set> activeTransactions;

    private static final Logger logger = FileLogger.getLogger(XIDManager.class);

    public XIDManager(){
        xid_counter = new AtomicInteger(0);
        activeTransactions = new ConcurrentHashMap<>();
    }

    public int newTransaction() {
        int xid = xid_counter.getAndAdd(1);
        logger.info("New xid=" + xid);
        activeTransactions.put(xid, ConcurrentHashMap.newKeySet(4));
        return xid;
    }

    public boolean validate(JSONObject request) throws JSONException {
        if(request.get(TYPE).equals(TRANSACTION) && request.get(ACTION).equals(NEW_TRANSACTION)) {
            return true;
        }
        if(request.get(TYPE).equals(OTHERS) && request.get(ACTION).equals(SHUTDOWN)) {
            return true;
        }
        if(activeTransactions.containsKey(request.getInt(XID))){
            return true;
        }
        logger.severe("validation failed");
        return false;
    }

    public Set<String> completeTransaction(int xid) {
        logger.info("Completing transaction xid=" + xid);
        Set rms = activeTransactions.remove(xid);
        logger.info("RMs involved " + rms);
        return rms;

    }

    public void addRM(int xid, String RM) {
        activeTransactions.get(xid).add(RM);
    }

}
