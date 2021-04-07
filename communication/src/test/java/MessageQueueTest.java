import hkube.communication.streaming.Flow;
import hkube.communication.streaming.Message;
import hkube.communication.streaming.MessageQueue;
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

public class MessageQueueTest {
    private Map<String, Object> flows;

    @Before
    public void beforeAll() throws IOException, URISyntaxException {
        JSONObject flowsJson = parseJSON("flows.json");
        flows = flowsJson.toMap();
    }

    @Test
    public void testQueue() {
        List consumers = new ArrayList();
        consumers.add("C");
        consumers.add("B");
        MessageQueue messageQueue = new MessageQueue("A", consumers, 1000000.0);

        List flowList = (List) flows.get("analyze");
        Flow flow = new Flow(flowList);
        flowList = (List) flows.get("master");
        Flow flow2 = new Flow(flowList);
        for (int i = 0; i < 10; i++) {
            Message msg = new Message((i + "").getBytes(), new byte[0], flow);
            msg.setProduceTime(System.currentTimeMillis());
            messageQueue.push(msg);
            msg = new Message((i + "").getBytes(), new byte[0], flow2);
            msg.setProduceTime(System.currentTimeMillis());
            messageQueue.push(msg);
        }

        messageQueue.pop("C");
        Message msg = messageQueue.pop("C");
        assert new String(msg.getData()).equals("1");
        messageQueue.pop("C");

        messageQueue.pop("B");
        messageQueue.pop("B");
        msg = messageQueue.pop("B");
        assert new String(msg.getData()).equals("2");


    }

    public JSONObject parseJSON(String filename) throws JSONException, IOException, URISyntaxException {
        String content = new String(Files.readAllBytes(Paths.get(getClass().getClassLoader()
                .getResource(filename)
                .toURI())));
        return new JSONObject(content);
    }
}
