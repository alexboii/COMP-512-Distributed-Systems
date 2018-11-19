package Persistence;

import Constants.GeneralConstants;

import java.io.*;

/**
 * Created by alex on 11/18/18.
 */
public class PersistedFile<T> implements Serializable {
    private File file;

    public PersistedFile(String name) {
        this.file = new File(File.separator
                + GeneralConstants.TEMP_FOLDER
                + File.separator + name + "_"
                + GeneralConstants.groupPrefix
                + GeneralConstants.DATA_EXTENSION);
    }

    public void save(T data) throws IOException {
        file.getParentFile().mkdirs();
        file.createNewFile();

        FileOutputStream fos = new FileOutputStream(file);
        ObjectOutputStream oos = new ObjectOutputStream(fos);
        oos.writeObject(data);

        oos.close();
    }

    public T read() throws IOException, ClassNotFoundException {
        FileInputStream fis = new FileInputStream(file);
        ObjectInputStream ois = new ObjectInputStream(fis);

        T obj = (T) ois.readObject();
        ois.close();

        return obj;
    }
}
