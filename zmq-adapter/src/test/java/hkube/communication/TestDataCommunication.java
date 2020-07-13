package hkube.communication;

import hkube.communication.zmq.ZMQRequest;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import hkube.communication.zmq.ZMQServer;
import org.junit.After;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
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
        CommConfig conf = new CommConfig();
        server = new ZMQServer(conf);
        DataServer ds = new DataServer(server,conf);
        JSONObject data1 = parseJSON("data1.json");
        ds.addTaskData("taskId1",data1.toMap());
        ZMQRequest zmqr = new ZMQRequest("localhost", conf.getListeningPort(), conf);
        DataRequest request = new SingleRequest(zmqr,"taskId1","level1.level2","msgpack");
        Object result = request.send();
        request.close();
        assert ((Map)result).get("value2").equals("d2_l1_l2_value_2");
        byte[] data2 = new byte[200];
        data2[1]=5;
        data2[2]=6;
        ds.addTaskData("taskId2",data2);
        zmqr = new ZMQRequest("localhost", conf.getListeningPort(), conf);
        request = new SingleRequest(zmqr,"taskId2",null,"msgpack");
        result =   request.send();
        request.close();
        ByteBuffer buf = (ByteBuffer) result;
        result = new byte[buf.remaining()];
        buf.get((byte[])result);
        assert ((byte[])result)[1] == 5;
        assert ((byte[])result)[2] == 6;
        zmqr = new ZMQRequest("localhost", conf.getListeningPort(), conf);
        request = new SingleRequest(zmqr,"taskId1",null,"msgpack");
        result= request.send();
        JSONObject resultAsJson = new JSONObject((Map)result);
        assert resultAsJson.query("/level1/value1").equals("d2_l1_value_1");
    }

    @Test
    public void getBatch() throws IOException, URISyntaxException, TimeoutException {
        CommConfig conf = new CommConfig();
        server = new ZMQServer(conf);
        DataServer ds = new DataServer(server,conf);
        JSONObject data1 = parseJSON("data1.json");
        ds.addTaskData("taskId1",data1.toMap());
        ZMQRequest zmqr = new ZMQRequest("localhost", conf.getListeningPort(), conf);
        List tasks = new ArrayList();
        tasks.add("taskId1");
        BatchRequest request = new BatchRequest(zmqr,tasks,"level1.level2","msgpack");
        Map result = request.send();
        request.close();
        assert ((Map)result.get("taskId1")).get("value2").equals("d2_l1_l2_value_2");

    }


    public JSONObject parseJSON(String filename) throws JSONException, IOException, URISyntaxException {
        String content = new String(Files.readAllBytes(Paths.get(getClass().getClassLoader()
                .getResource(filename)
                .toURI())));
        return new JSONObject(content);
    }


}

