package hkube.communication;

public interface ICommConfig {
    public Integer getMaxCacheSize();
    public String getListeningPort();
    public Integer getTimeout();
    public String getListeningHost();
    public String getEncodingType();
}
