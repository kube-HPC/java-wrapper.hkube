package hkube.communication.streaming;

public interface IProducer {
    public void produce(Message msg);
    public void start();
    public int getQueueSize(String consumer);
    public int getSent(String consumer);
    public  int getDropped(String consumer);
    public void registerResponseAccumulator(IResponseAccumulator accumulator);
}
