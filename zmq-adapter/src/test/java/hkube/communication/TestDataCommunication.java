package hkube.communication;

import hkube.caching.Cache;
import hkube.communication.zmq.ZMQRequest;
import hkube.encoding.EncodingManager;
import hkube.model.HeaderContentPair;
import hkube.model.ObjectAndSize;
import org.json.JSONException;
import org.json.JSONObject;
import hkube.communication.zmq.ZMQServer;
import org.junit.After;
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


public class TestDataCommunication {
    ZMQServer server;

    @After
    public void closeResources() {
        if (server != null) {
            try {
                server.close();
            } finally {
                server = null;
            }
        }
        else System.out.println("server is null");
    }

    @Test
    public void getDataOldTaskId() throws IOException, URISyntaxException, TimeoutException {
        Cache.init(4);
        EncodingManager encodingManager = new EncodingManager("msgpack");
        byte[] header = encodingManager.createHeader(false);
        CommConfig conf = new CommConfig();
        server = new ZMQServer(conf);
        DataServer ds = new DataServer(server,conf);
        ZMQRequest zmqr;
        byte[] data2 = new byte[200];
        data2[1]=5;
        data2[2]=6;
        HeaderContentPair pair = new HeaderContentPair(header,data2);
        ds.addTaskData("taskId2",pair);
        zmqr = new ZMQRequest("localhost", conf.getListeningPort(), conf);
        SingleRequest request = new SingleRequest(zmqr,"taskId2","msgpack");
        ObjectAndSize result =   (ObjectAndSize)request.send();

        request.close();
        assert ((byte[])result.getValue())[1] == 5;
        assert ((byte[])result.getValue())[2] == 6;
    }

    @Test
    public void getBatch() throws IOException, URISyntaxException, TimeoutException {
        EncodingManager encodingManager = new EncodingManager("msgpack");
        byte[] header = encodingManager.createHeader(false);
        Cache.init(4);
        CommConfig conf = new CommConfig();
        server = new ZMQServer(conf);
        DataServer ds = new DataServer(server,conf);
        byte[] data2 = new byte[200];
        data2[1]=5;
        data2[2]=6;
        HeaderContentPair pair = new HeaderContentPair(header,data2);
        ds.addTaskData("taskId2",pair);
        ZMQRequest zmqr = new ZMQRequest("localhost", conf.getListeningPort(), conf);
        List tasks = new ArrayList();
        tasks.add("taskId2");
        BatchRequest request = new BatchRequest(zmqr,tasks,"msgpack");
        Map resultMap =   request.send();
        Object result = resultMap.get("taskId2");
        request.close();
        assert ((byte[])result)[1] == 5;
        assert ((byte[])result)[2] == 6;
        JSONObject data1 = parseJSON("data1.json");
        ds.addTaskData("taskId1",encodingManager.encodeSeparately(data1.toMap()));
        zmqr = new ZMQRequest("localhost", conf.getListeningPort(), conf);
        tasks = new ArrayList();
        tasks.add("taskId1");
        request = new BatchRequest(zmqr,tasks,"msgpack");
        result = request.send();
        request.close();
        JSONObject resultAsJson = new JSONObject((Map)result);
        assert resultAsJson.query("/taskId1/level1/level2/value2").equals("d2_l1_l2_value_2");
    }

    public JSONObject parseJSON(String filename) throws JSONException, IOException, URISyntaxException {
        String content = new String(Files.readAllBytes(Paths.get(getClass().getClassLoader()
                .getResource(filename)
                .toURI())));
        return new JSONObject(content);
    }
}

