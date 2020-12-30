package hkube.communication;

public interface ICommConfig {
    public Integer getMaxCacheSize();
    public String getListeningPort();
    public Integer getStreamMaxBufferSize() ;
    public String getStreamListeningPort();
    public Integer getstreamstatisticsinterval();
    public Boolean isStateful();
    public Integer getTimeout();
    public Integer getNetworkTimeout();
    public String getListeningHost();
    public String getEncodingType();
}
