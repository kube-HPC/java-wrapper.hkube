package hkube.communication.streaming;

public interface IMessageListener {
    public void onMessage(Object msg, String origin);
}
