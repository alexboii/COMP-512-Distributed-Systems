package Utilities;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;

import static Constants.GeneralConstants.GROUP_PREFIX;

public class FileLogger {

    private static FileHandler handler = null;

    public static Logger getLogger(Class clazz) {
        Logger logger = Logger.getLogger(clazz.getName());
        if (handler == null) {
            try {
                handler = new FileHandler("/tmp/" + GROUP_PREFIX + ".log", true);
                VerySimpleFormatter sf = new VerySimpleFormatter();
                handler.setFormatter(sf);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        logger.addHandler(handler);
        return logger;
    }

    public static class VerySimpleFormatter extends Formatter {

        private static final String PATTERN = "[yyyy-MM-dd]' '[HH:mm:ss.SSS]";

        @Override
        public String format(final LogRecord record) {
            return String.format(
                    "%1$s %2$-7s %3$s\n",
                    new SimpleDateFormat(PATTERN).format(
                            new Date(record.getMillis())),
                    record.getLevel().getName(), formatMessage(record));
        }
    }
}
