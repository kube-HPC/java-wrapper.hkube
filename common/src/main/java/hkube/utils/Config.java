package hkube.utils;

import java.io.IOException;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Config {

    private final Logger logger;
    Properties configs;
    public Config() {
        logger = LogManager.getLogger();
        configs = new Properties();
        try {
            configs.load(this.getClass().getClassLoader().getResourceAsStream("config.properties"));
        } catch (Throwable e) {
            logger.info("no config in classpath");
        }
        ;
    }

    protected String getStrEnvValue(String name, String defaultValue) {
        String value = getValue(name);
        if (value == null) {
            if (defaultValue == null) {
                throw new RuntimeException("Missing environment parameter " + name);
            }
            return defaultValue;
        } else return value;
    }

    protected Integer getNumericEnvValue(String name, Integer defaultValue) {
        String value = getValue(name);
        if (value == null) {
            if (defaultValue == null) {
                throw new RuntimeException("Missing environment parameter " + name);
            }
            return defaultValue;
        }
        Integer asIntValue;

        asIntValue = Integer.valueOf(value);


        return asIntValue;
    }

    private String getValue(String name) {
        String value =(String) configs.get(name);
        if (value == null){
            value = System.getenv(name);
        }
        return value;
    }

}
