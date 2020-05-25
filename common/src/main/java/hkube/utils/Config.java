package hkube.utils;

public class Config {
   public  String getStrEnvValue(String name ,String defaultValue){
        String value =  System.getenv(name);
        if (value == null){
            if (defaultValue == null){
                throw new RuntimeException("Missing environment parameter "+ name);
            }
            return defaultValue;
        }
        else return  value;
    }
}
