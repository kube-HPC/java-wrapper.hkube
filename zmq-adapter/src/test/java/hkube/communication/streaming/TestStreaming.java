package hkube.communication.streaming;

import hkube.communication.streaming.zmq.Listener;
import hkube.communication.streaming.zmq.Producer;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class TestStreaming {
    Map flows;

    @Before
    public void beforeAll() throws IOException, URISyntaxException {
        JSONObject flowsJson = parseJSON("flows.json");
        flows = flowsJson.toMap();
    }






    @Test
    public void testSendReceive() throws TimeoutException {
        List concumers = new ArrayList();
        IResponseAccumulator accumulator = new IResponseAccumulator() {
            @Override
            public void onResponse(byte[] response, String origin) {

            }
        };

        concumers.add("a");
        concumers.add("b");
        IMessageHandler messageHandler = new IMessageHandler() {
            @Override
            public byte[] onMessage(Message message) {
                System.out.print(new String(message.data));
                System.out.print(new String(message.header));
                return "response".getBytes();
            }
        };

        Producer producer = new Producer("c","4004",accumulator,concumers,"msgpack");
        Listener listener = new Listener("localhost","4004",messageHandler,"msgpack","A");
        ArrayList next = new ArrayList();
        next.add("a");
        next.add("h");
        List flowList = (List)flows.get("analyze");
        Flow flow = new Flow(flowList);
        Message msg = new Message("Hello".getBytes(),"Header".getBytes(),flow);
        producer.start();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        listener.start();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        producer.produce(msg);
        listener.start();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
    public JSONObject parseJSON(String filename) throws JSONException, IOException, URISyntaxException {
        String content = new String(Files.readAllBytes(Paths.get(getClass().getClassLoader()
                .getResource(filename)
                .toURI())));
        return new JSONObject(content);
    }
}
