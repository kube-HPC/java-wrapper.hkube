package hkube.algo.wrapper;

import hkube.utils.Config;

public class WrapperConfig extends Config {
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

    public String getStorageType(){
        return getStrEnvValue("STORAGE_TYPE","fs");
    }

    public String getEncodingType(){
        return "msgpack";
    }
    public String getStorageEncodingType(){
        return "json";
    }

    public String getStorageVersion(){
        return "v2";
    }

}
