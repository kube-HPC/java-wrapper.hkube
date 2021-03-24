package hkube.communication.streaming;

import java.util.ArrayDeque;

public interface IProducer {
    public void produce(Message msg);
    public void start();
    public void close(boolean forceStop);
    public int getQueueSize(String consumer);
    public ArrayDeque resetQueueTimeDurations(String consumer);
    public int getQueueSize();
    public int getSent(String consumer);
    public  int getDropped(String consumer);

    public void registerResponseAccumulator(IResponseAccumulator accumulator);
}
