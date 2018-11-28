package Persistence;

import Constants.GeneralConstants;
import Constants.TransactionConstants.FILE_PTR;
import Model.RMHashMap;

import java.io.*;

/**
 * Created by alex on 11/26/18.
 */
public class ShadowFile {
    private PersistedFile<FILE_PTR> persistedFilePtr;
    private FILE_PTR filePtr;
    private String name;

    public ShadowFile(String name) {
        this.name = name;
        this.persistedFilePtr = new PersistedFile<>(name, FILE_PTR.PTR.toString());
        this.loadPtr();
    }

    private void loadPtr() {
        if (persistedFilePtr.exists()) {
            try {
                this.filePtr = persistedFilePtr.read();

                if (this.filePtr == null) {
                    this.filePtr = FILE_PTR.A;
                    this.persistFP();
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            this.filePtr = FILE_PTR.A;
        }
    }

    private boolean persistFP() {
        try {
            persistedFilePtr.save(filePtr);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public RMHashMap readHM() {
        try {
            FileInputStream fis = new FileInputStream(this.getFileName());
            ObjectInputStream ois = new ObjectInputStream(fis);
            RMHashMap hm = (RMHashMap) ois.readObject();
            return hm;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }

    public boolean writeHM(RMHashMap hm) {
        try {
            File f = new File(this.getFileName());

            f.getParentFile().mkdirs();
            f.createNewFile();

            FileOutputStream fos = new FileOutputStream(f, false);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(hm);
            oos.close();

            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean writeCommit(RMHashMap hm) {
        this.filePtr = (this.filePtr == FILE_PTR.A) ? FILE_PTR.B : FILE_PTR.A;

        return writeHM(hm) && this.persistFP();
    }

    public RMHashMap restore() {
        return readHM();
    }

    private String getFileName() {

        return File.separator +
                GeneralConstants.TEMP_FOLDER +
                File.separator +
                name + "_" +
                filePtr.toString() + "_" +
                GeneralConstants.GROUP_PREFIX +
                GeneralConstants.DATA_EXTENSION;
    }

}
