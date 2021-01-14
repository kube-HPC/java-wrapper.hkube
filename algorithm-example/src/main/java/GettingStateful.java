import hkube.algo.wrapper.IAlgorithm;
import hkube.api.IHKubeAPI;
import hkube.communication.streaming.IStreamingManagerMsgListener;

import java.util.Map;

public class GettingStateful implements IAlgorithm {
    boolean active = false;

    @Override
    public void Init(Map args) {

    }

    @Override
    public Object Start(Map input, IHKubeAPI hkubeAPI) throws Exception {
        hkubeAPI.registerInputListener(new IStreamingManagerMsgListener() {
            @Override
            public void onMessage(Object msg, String origin) {
                System.out.print("Got message " + msg + " from " + origin);
            }
        });
        active = true;
        hkubeAPI.startMessageListening();
        while (active) {
            Thread.sleep(1);


        }
        return null;
    }

    @Override
    public void Stop() {
        active = false;
    }

    @Override
    public void Cleanup() {

    }
}
