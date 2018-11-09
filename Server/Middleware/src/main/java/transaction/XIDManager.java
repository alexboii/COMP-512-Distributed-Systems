package transaction;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static Constants.GeneralConstants.*;

public class XIDManager {

    private AtomicInteger xid_counter;
    private Set<Integer> activeTransactions;

    public XIDManager(){
        xid_counter = new AtomicInteger(0);
        activeTransactions = ConcurrentHashMap.newKeySet();
    }

    public int newTransaction() {
        int xid = xid_counter.getAndAdd(1);
        activeTransactions.add(xid);
        return xid;
    }

    public boolean validate(JSONObject request) throws JSONException {
        if(request.get(TYPE).equals(TRANSACTION) && request.get(ACTION).equals(NEW_TRANSACTION)) {
            return true;
        }
        if(activeTransactions.contains(request.getInt(XID))){
            return true;
        }
        return false;
    }

    public void completeTransaction(int xid) {
        activeTransactions.remove(xid);
    }
}
