package Persistence;

import Constants.GeneralConstants;
import Utilities.FileLogger;

import java.io.*;
import java.util.logging.Logger;

/**
 * Created by alex on 11/18/18.
 */
public class PersistedFile<T> implements Serializable {

    private static final long serialVersionUID = 7931026671330862900L;

    private File file;
    private String path;
    private static final Logger logger = FileLogger.getLogger(PersistedFile.class);

    public PersistedFile(String name, String type) {
        this.path = File.separator
                + GeneralConstants.TEMP_FOLDER
                + File.separator + name + "_"
                + type + "_"
                + GeneralConstants.GROUP_PREFIX
                + GeneralConstants.DATA_EXTENSION;
        this.file = new File(path);
    }

    public void save(T data) throws IOException {
        logger.info("Attempting to save file for " + this.path);


        file.getParentFile().mkdirs();
        file.createNewFile();

        FileOutputStream fos = new FileOutputStream(file);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(data);

        oos.close();
    }

    public T read() throws IOException, ClassNotFoundException {
        logger.info("Attempting to read file for " + this.path);

        FileInputStream fis = new FileInputStream(file);
        ObjectInputStream ois = new ObjectInputStream(fis);

        T obj = (T) ois.readObject();
        ois.close();

        return obj;
    }

    public boolean exists() {
        return file.exists();
    }
}
