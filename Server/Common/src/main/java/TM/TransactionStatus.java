package TM;

import Constants.TransactionConstants;
import Model.ResourceItem;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by alex on 11/23/18.
 */
public class TransactionStatus implements Serializable {

    private static final long serialVersionUID = 1415726875793785707L;

    private Map<String, ResourceItem> writeSet;
    private Set<String> deleteSet;
    private TransactionConstants.STATUS status;

    TransactionStatus() {
        this.writeSet = new ConcurrentHashMap<>();
        // keep this as hash set, otherwise it fails to persist
        this.deleteSet = new HashSet<>();
        this.status = TransactionConstants.STATUS.ACTIVE;
    }

    public Map<String, ResourceItem> getWriteSet() {
        return writeSet;
    }

    public Set<String> getDeleteSet() {
        return deleteSet;
    }

    public TransactionConstants.STATUS getStatus() {
        return status;
    }

    public void setStatus(TransactionConstants.STATUS status) {
        this.status = status;
    }
}