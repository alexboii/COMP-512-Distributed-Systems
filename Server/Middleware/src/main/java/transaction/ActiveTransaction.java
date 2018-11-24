package transaction;

import Constants.TransactionConstants;

import java.io.Serializable;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by alex on 11/23/18.
 */
public class ActiveTransaction implements Serializable {

    private static final long serialVersionUID = -3331230174832800387L;

    private Set participants;


    private TransactionConstants.STATUS status;

    ActiveTransaction() {
        this.participants = ConcurrentHashMap.newKeySet(4);
        this.status = TransactionConstants.STATUS.ACTIVE;
    }

    public void setParticipants(Set participants) {
        this.participants = participants;
    }

    public Set getParticipants() {
        return participants;
    }

    public void setStatus(TransactionConstants.STATUS status) {
        this.status = status;
    }

    public TransactionConstants.STATUS getStatus() {
        return status;
    }
}
