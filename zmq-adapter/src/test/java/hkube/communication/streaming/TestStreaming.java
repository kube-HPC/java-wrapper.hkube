package hkube.communication.streaming;

import hkube.algo.CommandResponseListener;
import hkube.algo.ICommandSender;
import hkube.communication.streaming.zmq.IReadyUpdater;
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
        int[] responses = {0};
        int[] messages = {0};
        List concumers = new ArrayList();
        concumers.add("B");
        IResponseAccumulator accumulator = (response, origin, duration) -> {
            if(new String(response).equals("response"))
            responses[0]++;
        };


        IMessageHandler messageHandler = message -> {
            if (new String(message.data).equals("Hello") && new String(message.header).equals("Header"))
                messages[0]++;
            return "response".getBytes();

        };

        Producer producer = new Producer("A", "4004",  concumers, "msgpack",10000, new ICommandSender() {
            @Override
            public void sendMessage(String command, Object data, boolean isError) {

            }

            @Override
            public void addResponseListener(CommandResponseListener listener) {

            }
        });
        producer.registerResponseAccumulator(accumulator);
        Listener listener = new Listener("localhost", "4004", "msgpack", "B",new IReadyUpdater(){
            @Override
            public void setOthersAsReady(IListener l) {

            }

            @Override
            public void setOthersAsNotReady(IListener l) {

            }
        },new ICommandSender() {
            @Override
            public void sendMessage(String command, Object data, boolean isError) {

            }

            @Override
            public void addResponseListener(CommandResponseListener listener) {

            }
        });
        listener.setMessageHandler(messageHandler);
        ArrayList next = new ArrayList();
        next.add("A");
        next.add("H");
        List flowList = (List) flows.get("analyze");
        Flow flow = new Flow(flowList);
        Message msg = new Message("Hello".getBytes(), "Header".getBytes(), flow);
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
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assert messages[0] == 1;
        assert responses[0] == 1;
        flowList = (List) flows.get("master");
        flow = new Flow(flowList);
        msg = new Message("Hello".getBytes(), "Header".getBytes(), flow);
        producer.produce(msg);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assert messages[0] == 1;
        assert responses[0] == 1;
        flowList = (List) flows.get("analyze");
        flow = new Flow(flowList);
        msg = new Message("Hello".getBytes(), "Header".getBytes(), flow);
        producer.produce(msg);
        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assert messages[0] == 2;
        assert responses[0] == 2;
        producer.close(false);

    }

    public JSONObject parseJSON(String filename) throws JSONException, IOException, URISyntaxException {
        String content = new String(Files.readAllBytes(Paths.get(getClass().getClassLoader()
                .getResource(filename)
                .toURI())));
        return new JSONObject(content);
    }
}

