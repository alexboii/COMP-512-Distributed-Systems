package transaction;


import java.util.concurrent.atomic.AtomicInteger;

public class XIDManager {

    static AtomicInteger xid_counter = new AtomicInteger(0);

    public int newTransaction() {
        return xid_counter.getAndAdd(1);
    }

    //TODO: maintain active transactions for validation
}
