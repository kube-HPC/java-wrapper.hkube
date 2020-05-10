package hkube.algo.wrapper;

public class Config {
    String port;
    String host;
    String algorithmClassName;

    public String getPort() {
        return getStrEnvValue("WORKER_SOCKET_PORT","3000");
    }

    public String getHost() {
        return getStrEnvValue("WORKER_SOCKET_HOST","localhost");
    }

    public String getAlgorithmClassName() {

        return getStrEnvValue("ALGORITHM_ENTRY_POINT",null);
    }
    String getStrEnvValue(String name ,String defaultValue){
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
