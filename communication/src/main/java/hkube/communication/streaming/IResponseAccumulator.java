package hkube.communication.streaming;

public interface IResponseAccumulator {
    void onResponse(byte[] response, String origin, Long duration);
}
