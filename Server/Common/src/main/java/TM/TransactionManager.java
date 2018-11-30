package TM;

import Constants.ServerConstants;
import Constants.TransactionConstants.STATUS;
import LockManager.LockManager;
import Model.RMHashMap;
import Model.ResourceItem;
import Persistence.PersistedFile;
import Persistence.ShadowFile;
import TCP.RequestFactory;
import TCP.SocketUtils;
import Utilities.FileLogger;
import LockManager.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static Constants.GeneralConstants.*;
import static TCP.SocketUtils.sendRequest;

/**
 * Implements 2 Phase Locking
 */
public class TransactionManager {

    private LockManager lockManager;
    private Map<Integer, TransactionStatus> transactionStatus;
    private PersistedFile<Map<Integer, TransactionStatus>> persistedTransactionStatus;
    private RMHashMap mData;
    private ShadowFile persistedCommittedData;
    private String rmName;

    private static final Logger logger = FileLogger.getLogger(TransactionManager.class);

    public TransactionManager(String rmName, AtomicInteger rMCrashMode) {
        this.lockManager = new LockManager();
        this.transactionStatus = new ConcurrentHashMap<>();
        this.mData = new RMHashMap();
        this.rmName = rmName;

        this.persistedTransactionStatus = new PersistedFile<>(rmName, SNAPSHOT_FLAG);
        this.persistedCommittedData = new ShadowFile(rmName);

        this.loadData(rMCrashMode);
    }

    private void loadData(AtomicInteger rMCrashMode) {

        logger.info("Loading data for " + this.rmName);

        try {
            if (this.persistedTransactionStatus.exists()) {
                this.transactionStatus = this.persistedTransactionStatus.read();

                if (transactionStatus != null && transactionStatus.size() != 0) {

                    this.transactionStatus.keySet().forEach(key -> {

                        switch (this.transactionStatus.get(key).getStatus()) {
                            case ABORTED:
                            case COMMITTED:
                                sendDecisionFinalizedSignal(key);
                                break;
                            case UNCERTAIN:
                                JSONObject result = null;

                                // attempt to connect to middleware while this is null
                                while (result == null) {
                                    result = getDecision(key);
                                }

                                try {
                                    if (result.getBoolean(RESULT)) {
                                        commit(key);
                                    } else {
                                        abort(key);
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }

                                break;
                            case PREPARED:
                                abort(key);
                                break;
                            default:
                                break;
                        }

                        if (rMCrashMode.get() == 5) {
                            //Crash during recovery
                            logger.info("Simulating Resource Manager crash mode=" + rMCrashMode);
                            System.exit(1);
                        }
                    });
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            logger.info("Unable to load data for " + this.rmName);
            e.printStackTrace();
        }

        //still crash during recovery if there were no transaction statuses
        if (rMCrashMode.get() == 5) {
            //Crash during recovery
            logger.info("Simulating Resource Manager crash mode=" + rMCrashMode);
            System.exit(1);
        }

        RMHashMap hm = this.persistedCommittedData.restore();

        this.mData = hm == null ? new RMHashMap() : hm;

    }

    public JSONObject getDecision(int xid) {
        JSONObject result = null;

        try {
            Socket server = new Socket(InetAddress.getByName(ServerConstants.MIDDLEWARE_SERVER_ADDRESS), ServerConstants.MIDDLEWARE_PORT);

            OutputStreamWriter writer = new OutputStreamWriter(server.getOutputStream(), CHAR_SET);
            BufferedReader reader = new BufferedReader(new InputStreamReader(server.getInputStream(), CHAR_SET));

            result = SocketUtils.sendAndReceive(RequestFactory.getGetDecisionRequest(xid), writer, reader);

            server.close();
            writer.close();
            reader.close();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }

        return result;
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

        if (transactionStatus.get(xid) != null && transactionStatus.get(xid).getDeleteSet() != null && transactionStatus.get(xid).getDeleteSet().contains(key)) {
            return null;
        }

        if (transactionStatus.get(xid) != null && transactionStatus.get(xid).getWriteSet() != null && transactionStatus.get(xid).getWriteSet().get(key) != null) {
            return transactionStatus.get(xid).getWriteSet().get(key);
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
        transactionStatus.computeIfAbsent(xid, k -> new TransactionStatus());
        transactionStatus.get(xid).getWriteSet().put(key, value);

        if (transactionStatus.get(xid).getDeleteSet().contains(xid)) {
            transactionStatus.get(xid).getDeleteSet().remove(key);
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

        if (transactionStatus.get(xid) != null) {
            transactionStatus.get(xid).getWriteSet().remove(key);
            transactionStatus.get(xid).getDeleteSet().add(key);

            this.persistSnapshot();
        }

    }

    public boolean voteReply(int xid) {
        // TODO: Unsure -- should we be persisting all transactions at the same time, or just the transaction with which the reply is concerned?
        // Also, if something comes during this time, (another request) for said transaction, do we ignore it? do we care?
        transactionStatus.get(xid).setStatus(STATUS.PREPARED);

        transactionStatus.get(xid).setStatus(STATUS.UNCERTAIN);
        return this.persistSnapshot();
    }


    public boolean persistSnapshot() {
        try {
            synchronized (transactionStatus) {
                this.persistedTransactionStatus.save(transactionStatus);
            }
            logger.info("Successfully saved snapshot for " + rmName);

        } catch (IOException e) {
            e.printStackTrace();
            logger.info("Unable to write snapshot data to disk for " + rmName);
            return false;
        }

        return true;
    }

    public boolean persistData() {
        synchronized (mData) {
            if (this.persistedCommittedData.writeCommit(mData)) {
                logger.info("Successfully saved committed data for " + rmName);

                return true;
            }

        }
        logger.info("Failed to save committed data for " + rmName);
        return false;
    }

    public boolean commit(int xid) {

        logger.info("Committing xid: " + xid);

        if (transactionStatus.get(xid) != null) {
            transactionStatus.get(xid).setStatus(STATUS.COMMITTED);

            transactionStatus.get(xid).getWriteSet().forEach((key, value) -> commitData(key, value));
            transactionStatus.get(xid).getDeleteSet().forEach(key -> removeDataAndCommit(key));

            clear(xid);
            this.persistSnapshot();

            Boolean res = this.persistData();

            sendDecisionFinalizedSignal(xid);

            return res;
        }

        return true;
    }

    public boolean abort(int xid) {
        logger.info("Aborting xid: " + xid);

        if (transactionStatus.get(xid) != null) {
            transactionStatus.get(xid).setStatus(STATUS.ABORTED);

            clear(xid);

            Boolean res = this.persistSnapshot();

            sendDecisionFinalizedSignal(xid);

            return res;
        }


        return true;
    }

    private void sendDecisionFinalizedSignal(int xid) {
        try {
            JSONObject req = RequestFactory.getHaveCommittedRequest(xid, getAddress());
            sendRequest(ServerConstants.MIDDLEWARE_SERVER_ADDRESS, ServerConstants.MIDDLEWARE_PORT, req);
            logger.info("Sent finalized decision signal for xid: " + xid);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void clear(int xid) {
        if (transactionStatus.get(xid) != null) {
            logger.info("Removing local WriteSet(xid=" + xid + ") = " + transactionStatus.get(xid).getWriteSet());
            logger.info("Removing local DeleteSet(xid=" + xid + ") = " + transactionStatus.get(xid).getDeleteSet());

            transactionStatus.remove(xid);

            logger.info("Releasing all locks held by transaction: " + xid);

            lockManager.UnlockAll(xid);
        }
    }

    private String getAddress() {
        switch (rmName) {
            case "Flights":
                return ServerConstants.FLIGHTS_SERVER_ADDRESS + ":" + ServerConstants.FLIGHTS_SERVER_PORT;
            case "Cars":
                return ServerConstants.CAR_SERVER_ADDRESS + ":" + ServerConstants.CAR_SERVER_PORT;
            case "Rooms":
                return ServerConstants.ROOMS_SERVER_ADDRESS + ":" + ServerConstants.ROOMS_SERVER_PORT;
            default:
                return null;
        }
    }

}
