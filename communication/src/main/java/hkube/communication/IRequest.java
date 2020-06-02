package hkube.communication;

import java.util.concurrent.TimeoutException;

public interface IRequest {
    public byte[] send(byte[] data) throws TimeoutException ;
    public void close();
}
