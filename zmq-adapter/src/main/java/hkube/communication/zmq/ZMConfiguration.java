package hkube.communication.zmq;
import hkube.utils.Config;
public class ZMConfiguration extends Config{
    public String getListeningPort(){
        return getStrEnvValue("DISCOVERY_PORT","9020");
    }
    public Integer getTimeout(){
        return getNumericEnvValue("TIMEOUT",20);
    }
}
