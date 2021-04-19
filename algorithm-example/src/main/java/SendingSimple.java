import hkube.algo.wrapper.IAlgorithm;
import hkube.api.IHKubeAPI;

import java.util.*;

public class SendingSimple implements IAlgorithm {
    boolean active = false;

    @Override
    public void Init(Map args) {

    }

    @Override
    public Object Start(Map input, IHKubeAPI hkubeAPI) throws Exception {
        int rng = (int) ((Map) ((List) input.get("input")).get(0)).get("rng");
        int burst = (int) ((Map) ((List) input.get("input")).get(0)).get("burst");
        List sleepTime = (List) ((Map) ((List) input.get("input")).get(0)).get("sleepTime");
        int totalMsg = (int) ((Map) ((List) input.get("input")).get(0)).get("totalMsg");
        String nodeName = (String) input.get("nodeName");


        int r = rng;
        int sent = 0;
        active = true;
        while (active) {
            if (active) {
                Map msg = new HashMap();
                List arrList = new ArrayList();
                arrList.add(nodeName);
                msg.put("trace", arrList);
                msg.put("data", new byte[900]);
                msg.put("id", ++sent);
                msg.put("time", new Date().getTime());
                hkubeAPI.sendMessage(msg, "analyze");
                hkubeAPI.sendMessage(msg);
            }
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
