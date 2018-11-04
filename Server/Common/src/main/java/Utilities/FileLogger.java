package Utilities;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;

import static Constants.GeneralConstants.groupPrefix;

public class FileLogger {

    public static Logger getLogger(Class clazz) {
        Logger logger = Logger.getLogger(clazz.getName());
        FileHandler handler = null;
        try {
            handler = new FileHandler("/tmp/" + groupPrefix + ".log", true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.addHandler(handler);
        return logger;
    }
}
