package hkube.communication.streaming;

public interface IListener {
    public void start();
    public void setMessageHandler(IMessageHandler handler);
}
