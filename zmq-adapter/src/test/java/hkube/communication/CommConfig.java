package hkube.communication;

import hkube.utils.Config;

public class CommConfig extends Config implements ICommConfig {

    @Override
    public Integer getMaxCacheSize() {
        return getNumericEnvValue("DISCOVERY_MAX_CACHE_SIZE", 3);
    }
    @Override
    public String getListeningPort(){
        return getStrEnvValue("DISCOVERY_PORT","9020");
    }
    @Override
    public Integer getTimeout(){
        return getNumericEnvValue("TIMEOUT",20000);
    }

    @Override
    public String getListeningHost() {
        return "localhost";
    }
}