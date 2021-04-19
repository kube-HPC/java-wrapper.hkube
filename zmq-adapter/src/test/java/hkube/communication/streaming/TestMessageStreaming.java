package hkube.communication.streaming;

import hkube.algo.CommandResponseListener;
import hkube.algo.ICommandSender;
import hkube.communication.CommConfig;
import hkube.communication.streaming.zmq.Listener;
import hkube.communication.streaming.zmq.Producer;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestMessageStreaming {
    Map flows;

    @Before
    public void beforeAll() throws IOException, URISyntaxException {
        JSONObject flowsJson = parseJSON("flows.json");
        flows = flowsJson.toMap();
    }

    private ICommandSender handler = new ICommandSender() {
        @Override
        public void sendMessage(String command, Object data, boolean isError) {
            System.out.print("error " + data);
        }

        @Override
        public void addResponseListener(CommandResponseListener listener) {

        }
    };

    @Test
    public void testStatistics() {
        List<Statistics>[] stats = new ArrayList[1];
        Map[] data = new HashMap[1];
        String[] origins = new String[1];
        Integer[] allDurations = new Integer[1];
        allDurations[0] = 0;
        CommConfig conf = new CommConfig();
        List concumers = new ArrayList();
        concumers.add("B");
        concumers.add("C");
        Producer producer = new Producer("A", "4004", concumers, "msgpack", 10000, handler);
        MessageProducer msgProducer = new MessageProducer(producer, conf, concumers);
        IStatisticsListener statsListener = new IStatisticsListener() {
            @Override
            public void onStatistics(List<Statistics> statistics) {
                stats[0] = statistics;
                if (statistics.get(0).durations != null) {
                    allDurations[0] += statistics.get(0).durations.size();
                }
            }
        };
        msgProducer.registerStatisticsListener(statsListener);
        IListener listener = new Listener("localhost", "4004", "msgpack", "B", handler);
        MessageListener msgListener = new MessageListener(conf, listener, "A");
        msgListener.register(new IMessageListener() {
            @Override
            public void onMessage(Object msg, Flow flow, String origin) {
                data[0] = (Map) msg;
                origins[0] = origin;
            }
        });
        msgProducer.start();


        List flowList = (List) flows.get("analyze");
        Flow flow = new Flow(flowList);
        HashMap myMap = new HashMap();
        myMap.put("field1", "value1");
        msgProducer.produce(flow, myMap);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assert stats[0].get(0).sent == 0;
        assert stats[0].get(0).queueSize == 1;
        assert stats[0].get(1).sent == 0;
        assert stats[0].get(1).queueSize == 0;

        assert allDurations[0] == 0;
        msgListener.start();
        msgListener.fetch();
        msgListener.fetch();

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assert stats[0].get(0).sent == 1;
        assert stats[0].get(0).queueSize == 0;
        assert stats[0].get(0).responses == 1;
        assert stats[0].get(1).sent == 0;
        assert stats[0].get(1).queueSize == 0;
        assert allDurations[0] == 1;
        assert data[0].get("field1").equals("value1");
        flowList = (List) flows.get("master");
        flow = new Flow(flowList);
        msgProducer.produce(flow, myMap);
        msgListener.fetch();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assert stats[0].get(0).sent == 1;
        assert stats[0].get(0).queueSize == 0;
        assert stats[0].get(0).responses == 1;
        assert stats[0].get(1).sent == 0;
        assert stats[0].get(1).queueSize == 1;

    }

    public JSONObject parseJSON(String filename) throws JSONException, IOException, URISyntaxException {
        String content = new String(Files.readAllBytes(Paths.get(getClass().getClassLoader()
                .getResource(filename)
                .toURI())));
        return new JSONObject(content);
    }
}
