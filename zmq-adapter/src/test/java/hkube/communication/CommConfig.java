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
    public Integer getStreamMaxBufferSize() {
        return 2;
    }

    @Override
    public String getStreamListeningPort() {
        return "4004";
    }

    @Override
    public Integer getstreamstatisticsinterval() {
        return 1;
    }

    @Override
    public Boolean isStateful() {
        return true;
    }

    @Override
    public Integer getTimeout(){
        return getNumericEnvValue("DISCOVERY_TIMEOUT",1000);
    }

    @Override
    public Integer getNetworkTimeout() {
        return getNumericEnvValue("DISCOVERY_NETWORK_TIMEOUT",1000);
    }

    @Override
    public String getListeningHost() {
        return "localhost";
    }

    @Override
    public String getEncodingType() {
        return "msgpack";
    }
}
