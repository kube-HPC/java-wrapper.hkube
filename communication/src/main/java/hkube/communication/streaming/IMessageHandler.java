package hkube.communication.streaming;

public interface IMessageHandler {
    byte[] onMessage(Message message);
}
