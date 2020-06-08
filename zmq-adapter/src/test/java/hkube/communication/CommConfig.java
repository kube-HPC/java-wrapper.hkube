package hkube.communication;

import hkube.utils.Config;

public class CommConfig extends Config implements ICommConfig {
    public Integer getMaxCacheSize() {
        return getNumericEnvValue("DISCOVERY_MAX_CACHE_SIZE", 3);
    }
    public String getListeningPort(){
        return getStrEnvValue("DISCOVERY_PORT","9020");
    }
    public Integer getTimeout(){
        return getNumericEnvValue("TIMEOUT",20000);
    }
}
