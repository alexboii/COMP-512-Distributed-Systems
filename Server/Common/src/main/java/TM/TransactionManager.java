package TM;

import LockManager.LockManager;
import Model.RMHashMap;
import Model.ResourceItem;
import Persistence.PersistedFile;
import Utilities.FileLogger;
import LockManager.*;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Implements 2 Phase Locking
 */
public class TransactionManager {

    private LockManager lockManager;
    private Map<Integer, Snapshot> snapshots;
    private PersistedFile<Map<Integer, Snapshot>> persistedSnapshot;
    private RMHashMap mData;
    private PersistedFile<RMHashMap> persistedCommitedData;
    private String rmName;

    private static final Logger logger = FileLogger.getLogger(TransactionManager.class);

    public TransactionManager(String rmName) {
        this.lockManager = new LockManager();
        this.snapshots = new ConcurrentHashMap<>();
        this.mData = new RMHashMap();
        this.rmName = rmName;

        this.persistedSnapshot = new Pers

    }

    private class Snapshot implements Serializable {
        private Map<String, ResourceItem> writeSet;
        private Set<String> deleteSet;

        Snapshot() {
            this.writeSet = new ConcurrentHashMap<>();
            this.deleteSet = ConcurrentHashMap.newKeySet();
        }

        public Map<String, ResourceItem> getWriteSet() {
            return writeSet;
        }

        public Set<String> getDeleteSet() {
            return deleteSet;
        }
    }

    /**
     * Reads a data item from global map
     *
     * @param xid
     * @param key
     * @return
     */
    public ResourceItem readCommittedData(int xid, String key) {
        synchronized (mData) {
            ResourceItem item = mData.get(key);
            if (item != null) {
                return (ResourceItem) item.clone();
            }
            return null;
        }
    }

    /**
     * Writes a data item to global map
     *
     * @param key
     * @param value
     */
    public void commitData(String key, ResourceItem value) {
        synchronized (mData) {
            mData.put(key, value);
        }
    }

    /**
     * Remove the item out of global map
     *
     * @param key
     */
    public void removeDataAndCommit(String key) {
        synchronized (mData) {
            mData.remove(key);
        }
    }

    /**
     * Reads data from transaction's local copy. If not available then reads data from the global copy
     *
     * @param xid
     * @param key
     * @return
     * @throws DeadlockException
     */
    public ResourceItem readDataTransaction(int xid, String key) throws DeadlockException {
        lockManager.Lock(xid, key, TransactionLockObject.LockType.LOCK_READ);

        if (snapshots.get(xid).getDeleteSet() != null && snapshots.get(xid).getDeleteSet().contains(key)) {
            return null;
        }

        if (snapshots.get(xid).writeSet != null && snapshots.get(xid).getWriteSet().get(key) != null) {
            return snapshots.get(xid).getWriteSet().get(key);
        }

        return readCommittedData(xid, key);
    }

    /**
     * Writes data to transaction's local copy
     *
     * @param xid
     * @param key
     * @param value
     * @throws DeadlockException
     */
    public void writeDataTransaction(int xid, String key, ResourceItem value) throws DeadlockException {
        lockManager.Lock(xid, key, TransactionLockObject.LockType.LOCK_WRITE);
        snapshots.computeIfAbsent(xid, k -> new Snapshot());
        snapshots.get(xid).getWriteSet().put(key, value);

        if (snapshots.get(xid).getDeleteSet().contains(xid)) {
            snapshots.get(xid).getDeleteSet().remove(key);
        }
    }

    /**
     * Removes data from transaction's local copy
     *
     * @param xid
     * @param key
     * @throws DeadlockException
     */
    public void removeDataTransaction(int xid, String key) throws DeadlockException {
        lockManager.Lock(xid, key, TransactionLockObject.LockType.LOCK_WRITE);

        if (snapshots.get(xid) != null) {
            snapshots.get(xid).getWriteSet().remove(key);
            snapshots.get(xid).getDeleteSet().add(key);
        }

    }

    public void commit(int xid) {

        logger.info("Committing xid: " + xid);

        if (snapshots.get(xid) != null) {
            snapshots.get(xid).getWriteSet().forEach((key, value) -> commitData(key, value));
            snapshots.get(xid).getDeleteSet().forEach(key -> removeDataAndCommit(key));
        }

        clear(xid);
    }

    public void abort(int xid) {
        logger.info("Aborting xid: " + xid);
        clear(xid);
    }

    private void clear(int xid) {
        logger.info("Removing local WriteSet(xid=" + xid + ") = " + snapshots.get(xid).getWriteSet());
        logger.info("Removing local DeleteSet(xid=" + xid + ") = " + snapshots.get(xid).getDeleteSet());

        snapshots.remove(xid);

        logger.info("Releasing all locks held by transaction: " + xid);
        lockManager.UnlockAll(xid);
    }

}
