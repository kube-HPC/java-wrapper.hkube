package hkube.communication.streaming;

public interface IStreamingManagerMsgListener {
    public void onMessage(Object msg, String origin);
}
