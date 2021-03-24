package hkube.communication.streaming.zmq;

import hkube.communication.streaming.IListener;

public interface IReadyUpdater {
    void setOthersAsReady(IListener exclude);
    void setOthersAsNotReady(IListener exclude);
}
