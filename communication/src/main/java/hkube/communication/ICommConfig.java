package hkube.communication;

public interface ICommConfig {
    public Integer getMaxCacheSize();
    public String getListeningPort();
    public Integer getTimeout();
    public Integer getNetworkTimeout();
    public String getListeningHost();
    public String getEncodingType();
}
