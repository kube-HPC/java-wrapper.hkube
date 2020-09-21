package hkube.communication;

import java.util.List;
import java.util.concurrent.TimeoutException;

public interface IRequest {
    public List<HeaderContentPair> send(byte[] data) throws TimeoutException ;
    public void close();
}
