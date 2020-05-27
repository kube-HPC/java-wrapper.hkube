package hkube.utils;

public class Config {
    protected String getStrEnvValue(String name, String defaultValue) {
        String value = System.getenv(name);
        if (value == null) {
            if (defaultValue == null) {
                throw new RuntimeException("Missing environment parameter " + name);
            }
            return defaultValue;
        } else return value;
    }

    protected Integer getNumericEnvValue(String name, Integer defaultValue) {
        String value = System.getenv(name);
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
}
