import hkube.algo.wrapper.IAlgorithm;
import hkube.api.IHKubeAPI;

import java.util.List;
import java.util.Map;

public class SleepAlg implements IAlgorithm {

    @Override
    public void Init(Map args) {

    }

    @Override
    public Object Start(Map input, IHKubeAPI hkubeAPI) throws Exception {
        Map msg = (Map) ((Map) input.get("streamInput")).get("message");
        String nodeName = (String) input.get("nodeName");
        msg.put("trace", msg.get("trace") + nodeName);
        System.out.println("handling " + msg.get("id") + " on " + msg.get("trace") + " " +System.getenv().get("POD_IP"));
        int rate = (int) ((Map) ((List) input.get("input")).get(0)).get("rate");
        Thread.sleep(1000 / rate);
        return msg;
    }

    @Override
    public void Stop() {

    }

    @Override
    public void Cleanup() {

    }
}
