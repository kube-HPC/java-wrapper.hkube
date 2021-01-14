package hkube.communication.streaming;

public interface IMessageListener {
    public void onMessage(Object msg, Flow flow, String origin);
}
