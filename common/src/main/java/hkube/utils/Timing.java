package hkube.utils;

import java.util.Date;

import org.apache.logging.log4j.Logger;

public class Timing {
    private Logger logger;
    private String name;
    long startTime = 0;
    long endTime = 0;

    public Timing(Logger logger, String name) {
        this.logger = logger;
        this.name = name;
    }

    public void start() {
        startTime = new Date().getTime();
    }

    public void end() {
        endTime = new Date().getTime();
    }

    public void logDebug() {
        logger.debug(getMessage());
    }

    public void logInfo() {
        logger.info(getMessage());
    }

    public void logWarn() {
        logger.warn(getMessage());
    }

    private String getMessage() {
        if (endTime == 0 || startTime == 0) {
            return "Time not reported for " + name + " startime: " + startTime + " endtime: " + endTime;
        }
        long duration = endTime - startTime;
        return name + " took " + duration + " milliseconds";
    }
}
