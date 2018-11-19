package TM;

import LockManager.LockManager;
import Model.RMHashMap;
import Model.ResourceItem;
import Persistence.PersistedFile;
import Utilities.FileLogger;
import LockManager.*;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static Constants.GeneralConstants.COMMITTED_FLAG;
import static Constants.GeneralConstants.SNAPSHOT_FLAG;

/**
 * Implements 2 Phase Locking
 */
public class TransactionManager {

    private LockManager lockManager;
    private transient Map<Integer, Snapshot> snapshots;
    private PersistedFile<Map<Integer, Snapshot>> persistedSnapshot;
    private transient RMHashMap mData;
    private PersistedFile<RMHashMap> persistedCommittedData;
    private String rmName;

    private static final Logger logger = FileLogger.getLogger(TransactionManager.class);

    public TransactionManager(String rmName) {
        this.lockManager = new LockManager();
        this.snapshots = new ConcurrentHashMap<>();
        this.mData = new RMHashMap();
        this.rmName = rmName;

        this.persistedSnapshot = new PersistedFile<>(rmName, SNAPSHOT_FLAG);
        this.persistedCommittedData = new PersistedFile<>(rmName, COMMITTED_FLAG);

        this.loadData();
    }

    private static class Snapshot implements Serializable {

        private static final long serialVersionUID = 1415726875793785707L;

        private transient Map<String, ResourceItem> writeSet;
        private transient Set<String> deleteSet;

        Snapshot() {
            this.writeSet = new ConcurrentHashMap<>();
            // keep this as hash set, otherwise it fails to persist
            this.deleteSet = new HashSet<>();
        }

        public Map<String, ResourceItem> getWriteSet() {
            return writeSet;
        }

        public Set<String> getDeleteSet() {
            return deleteSet;
        }
    }

    private void loadData() {
        try {
            logger.info("Loading data for " + this.rmName);
            this.snapshots = this.persistedSnapshot.read();
            this.mData = this.persistedCommittedData.read();
        } catch (IOException | ClassNotFoundException e) {
            logger.info("Unable to load data for " + this.rmName);
            e.printStackTrace();
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

        if (snapshots.get(xid) != null && snapshots.get(xid).getDeleteSet().contains(key)) {
            return null;
        }

        if (snapshots.get(xid) != null && snapshots.get(xid).getWriteSet().get(key) != null) {
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

        this.persistSnapshot();
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

            this.persistSnapshot();
        }

    }

    public void persistSnapshot() {
        try {
            this.persistedSnapshot.save(snapshots);
            logger.info("Successfully saved snapshot for " + rmName);
        } catch (IOException e) {
            logger.info("Unable to write snapshot to disk for " + rmName);
            e.printStackTrace();
        }
    }

    public void persistData() {
        try {
            this.persistedCommittedData.save(mData);
            logger.info("Successfully saved committed data for " + rmName);
        } catch (IOException e) {
            e.printStackTrace();
            logger.info("Unable to write committed data to disk for " + rmName);
        }
    }

    public void commit(int xid) {

        logger.info("Committing xid: " + xid);

        if (snapshots.get(xid) != null) {
            snapshots.get(xid).getWriteSet().forEach((key, value) -> commitData(key, value));
            snapshots.get(xid).getDeleteSet().forEach(key -> removeDataAndCommit(key));
        }

        clear(xid);
        this.persistData();
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
