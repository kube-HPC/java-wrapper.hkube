import hkube.algo.wrapper.IAlgorithm;
import hkube.api.IHKubeAPI;

import java.util.*;

public class SendingStateful implements IAlgorithm {
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
        int i = 0;
        int z = 0;
        active = true;
        while (active) {
            for (int c = 0; c < r; c++) {


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
            }
            i++;
            z += 1;
            if (i % 60 == 0) {
                z += 1;
                System.out.println("z=" + z);
            }
            if (i % 600 == 0) {
                System.out.print("******** start burst ********");
                r = rng * burst;
            }
            if (i % 900 == 0) {
                r = rng;
                i = 0;
                System.out.print("========== end burst ==========");
            }
            if (z > (int) sleepTime.get(0)) {
                z = 0;
                System.out.print("======= start sleep ========");
                Thread.sleep((Integer) sleepTime.get(1) * 1000);
                System.out.print("======= end sleep ========");
            }
            if (sent > totalMsg) {
                Thread.sleep( (Integer)sleepTime.get(1) * 1000);
                active = false;
            }
            Thread.sleep(1000);

        }
        Thread.sleep(120000);
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
