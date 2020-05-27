package hkube.communication;

public interface IRequestServer {
    public void addRequestsListener(IRequestListener listener);
    public void reply(byte[] reply);
    public void close();
}
