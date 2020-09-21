package hkube.communication;

import java.util.List;

public interface IRequestServer {
    public void addRequestsListener(IRequestListener listener);
    public void reply(List<HeaderContentPair> replies);
    public void close();
}
