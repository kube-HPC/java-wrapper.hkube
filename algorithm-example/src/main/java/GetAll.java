import hkube.algo.wrapper.IAlgorithm;
import hkube.api.IHKubeAPI;
import org.json.JSONObject;

import java.util.*;

public class GetAll implements IAlgorithm {
    Map<String, List> allInstances = new HashMap();
    Map<String, Long> stats = new HashMap<>();
    boolean active = false;


    @Override
    public void Init(Map args) {

    }

    void addToOrign(Map<String, Integer> dict, String key) {
        if (dict.containsKey(key)) {
            dict.put(key, dict.get(key) + 1);
        } else {
            dict.put(key, 1);
        }
    }


    void addInstaceToOrign(String trace, Integer id) {
        if (allInstances.containsKey(trace)) {

            allInstances.get(trace).add(id);
        } else {
            List initialList = new ArrayList();
            initialList.add(id);
            allInstances.put(trace, initialList);
        }
    }


    synchronized void addToTimeStats(String trace, long msgTime) {
        long now = new Date().getTime();
        long duration = now - msgTime;
        if (stats.keySet().contains(trace)) {
            stats.put(trace, ((stats.get(trace) * stats.get(trace + "_count") + duration) / (stats.get(trace + "_count") + 1)));
            stats.put(trace + "_count", stats.get(trace + "_count") + 1);
            stats.put(trace + "_min", Math.min(stats.get(trace + "_min"), duration));
            stats.put(trace + "_max", Math.max(stats.get(trace + "_max"), duration));
        } else {
            stats.put(trace, duration);
            stats.put(trace + "_count", (long) 1);
            stats.put(trace + "_min", duration);
            stats.put(trace + "_max", duration);

        }
    }


    @Override
    public Object Start(Map input, IHKubeAPI hkubeAPI) throws Exception {
        Map dic = new HashMap();
        Map traceMap = new HashMap();
        final String nodeName = (String) input.get("nodeName");
        hkubeAPI.registerInputListener((msg, origin) -> {
            addToOrign(dic, origin);
            String trace =((Map) msg).get("trace") + nodeName;
            System.out.println("handling " +((Map) msg).get("id") + " on " +trace);
            addToOrign(traceMap, trace);
            addInstaceToOrign(trace, (Integer) ((Map) msg).get("id"));
            addToTimeStats(trace, (Long) ((Map) msg).get("time"));
        });

        hkubeAPI.startMessageListening();
        active = true;
        while (active) {
            System.out.println(new JSONObject(dic));
            System.out.println("traces stats:" + new JSONObject(traceMap));
            System.out.println("duration stats" + new JSONObject(stats));

            synchronized (this.getClass()) {
                stats = new HashMap();
            }
            allInstances.entrySet().stream().forEach(entrty -> {


                        Integer c = 1;
                        for (; c < 100000; c++) {
                            if (!entrty.getValue().contains(c)) {
                                break;
                            }
                        }
                        if (c == 1) {
                            System.out.println("Found instances" + new JSONObject(entrty.getValue()));
                        } else {
                            System.out.println("Found up to " + c);
                        }

                    }
            );
            Thread.sleep(30000);
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
