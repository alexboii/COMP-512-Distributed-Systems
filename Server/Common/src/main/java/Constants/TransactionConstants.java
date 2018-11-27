package Constants;

import java.io.Serializable;

/**
 * Created by alex on 11/23/18.
 */
public class TransactionConstants {
    public enum STATUS implements Serializable {
        ACTIVE, COMMITTED, PREPARED, UNCERTAIN, ABORTED
    }

    public enum FILE_PTR implements Serializable {
        A, B, PTR
    }
}
