import hkube.algo.wrapper.IAlgorithm;
import hkube.api.IHKubeAPI;

import java.util.Map;

public class SendingStateful implements IAlgorithm {
    boolean active = false;
    @Override
    public void Init(Map args) {

    }

    @Override
    public Object Start(Map input, IHKubeAPI hkubeAPI) throws Exception {
        active = true;
        while(active){
            hkubeAPI.sendMessage("my message");
            Thread.sleep(1000);
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
