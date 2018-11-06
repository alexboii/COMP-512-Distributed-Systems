package TM;

import LockManager.LockManager;
import Model.RMHashMap;
import Model.ReservableItem;
import Model.ResourceItem;
import Utilities.FileLogger;
import LockManager.*;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Implements 2 Phase Locking
 */
public class TransactionManager {

    private LockManager lockManager;
    private Map<Integer, Map<String, ResourceItem>> writeSet;
    private Map<Integer, Set<String>> deleteSet;
    private RMHashMap m_data;

    private static final Logger logger = FileLogger.getLogger(TransactionManager.class);

    public TransactionManager() {
        lockManager = new LockManager();
        writeSet = new ConcurrentHashMap<>();
        deleteSet = new ConcurrentHashMap<>();
        m_data = new RMHashMap();
    }

    // Reads a data item
    public ResourceItem readCommittedData(int xid, String key) {
        synchronized (m_data) {
            ResourceItem item = m_data.get(key);
            if (item != null) {
                return (ResourceItem) item.clone();
            }
            return null;
        }
    }

    // Writes a data item
    public void commitData(int xid, String key, ResourceItem value) {
        synchronized (m_data) {
            m_data.put(key, value);
        }
    }

    // Remove the item out of storage
    public void removeData(int xid, String key) {
        synchronized (m_data) {
            m_data.remove(key);
        }
    }

    /**
     * Reads the transaction's local copy of data first if it is available, otherwise reads the committed copy
     * @param xid
     * @param key
     * @return
     * @throws DeadlockException
     */
    public ResourceItem readDataTransaction(int xid, String key) throws DeadlockException {
        lockManager.Lock(xid, key, TransactionLockObject.LockType.LOCK_READ);

        if(deleteSet.get(xid) != null && deleteSet.get(xid).contains(key)) {
            return null;
        }
        if(writeSet.get(xid) != null && writeSet.get(xid).get(key) != null) {
            return writeSet.get(xid).get(key);
        }

        return readCommittedData(xid, key);
    }

    public void removeDataTransaction(int xid, String key) throws DeadlockException {
        lockManager.Lock(xid, key, TransactionLockObject.LockType.LOCK_WRITE);

        if(writeSet.get(xid) != null) {
            writeSet.get(xid).remove(key);
        }

        if(deleteSet.get(xid) == null) {
            deleteSet.put(xid, ConcurrentHashMap.newKeySet());
        }
        deleteSet.get(xid).add(key);
    }

    /**
     * Writes data to local copy of transaction
     * @param xid
     * @param key
     * @param value
     * @throws DeadlockException
     */
    public void writeDataLocally(int xid, String key, ResourceItem value) throws DeadlockException {
        lockManager.Lock(xid, key, TransactionLockObject.LockType.LOCK_WRITE);
        if(writeSet.get(xid) == null) {
            writeSet.put(xid, new ConcurrentHashMap<>());
        }
        writeSet.get(xid).put(key, value);

        if (deleteSet.get(xid) != null) {
            deleteSet.get(xid).remove(key);
        }
    }

    //TODO: using this method for Car only first for testing
    public boolean deleteItemTransaction(int xid, String key) throws DeadlockException {
        logger.info("RM::deleteItem(" + xid + ", " + key + ") called");
        ReservableItem curObj = (ReservableItem) readDataTransaction(xid, key);
        // Check if there is such an item in the storage
        if (curObj == null) {
            logger.warning("RM::deleteItem(" + xid + ", " + key + ") failed--item doesn't exist");
            return false;
        } else {
            if (curObj.getReserved() == 0) {
                removeDataTransaction(xid, curObj.getKey());
                logger.info("RM::deleteItem(" + xid + ", " + key + ") item deleted");
                return true;
            } else {
                logger.info("RM::deleteItem(" + xid + ", " + key + ") item can't be deleted because some customers have reserved it");
                return false;
            }
        }
    }

    public boolean deleteItem(int xid, String key) {
        logger.info("RM::deleteItem(" + xid + ", " + key + ") called");
        ReservableItem curObj = (ReservableItem) readCommittedData(xid, key);
        // Check if there is such an item in the storage
        if (curObj == null) {
            logger.warning("RM::deleteItem(" + xid + ", " + key + ") failed--item doesn't exist");
            return false;
        } else {
            if (curObj.getReserved() == 0) {
                removeData(xid, curObj.getKey());
                logger.info("RM::deleteItem(" + xid + ", " + key + ") item deleted");
                return true;
            } else {
                logger.info("RM::deleteItem(" + xid + ", " + key + ") item can't be deleted because some customers have reserved it");
                return false;
            }
        }
    }

    public boolean abort(int xid) {
        logger.info("Aborting xid: " + xid);
        clear(xid);
        return true;
    }

    public boolean commit(int xid) {

        logger.info("Committing xid: " + xid);

        if(writeSet.get(xid) != null) {
            writeSet.get(xid).forEach((key, value) -> commitData(0, key, value));
        }

        if(deleteSet.get(xid) != null) {
            deleteSet.get(xid).forEach(key -> removeData(0, key));
        }

        clear(xid);
        return true;

    }

    private void clear(int xid) {
        logger.info("Removing local WriteSet(xid=" + xid +") = " + writeSet.get(xid));
        logger.info("Removing local DeleteSet(xid=" + xid +") = " + deleteSet.get(xid));

        writeSet.remove(xid);
        deleteSet.remove(xid);

        logger.info("Releasing all locks held by transaction: " + xid);
        lockManager.UnlockAll(xid);
    }

}
