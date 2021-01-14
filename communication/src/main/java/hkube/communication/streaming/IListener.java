package hkube.communication.streaming;

public interface IListener {
    public void start();
    public void close();
    public void setMessageHandler(IMessageHandler handler);
}
