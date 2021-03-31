package hkube.communication.streaming;

public interface IListener {
    public void start();
    public void fetch();
    public void close(boolean forceCLose);
    public void setMessageHandler(IMessageHandler handler);
    public void ready(boolean isReady);
    public String getId();
}
