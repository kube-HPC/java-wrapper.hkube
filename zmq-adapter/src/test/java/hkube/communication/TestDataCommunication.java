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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
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
        ds.addTaskData("taskId1",data1);
        ZMQRequest zmqr = new ZMQRequest("localhost", conf.getListeningPort(), conf);
        DataRequest request = new DataRequest(zmqr,"taskId1",null,"level1.level2","msgpack");
        Object result = request.send();
        request.close();
        assert ((JSONObject)result).get("value2").equals("d2_l1_l2_value_2");
        byte[] data2 = new byte[200];
        data2[1]=5;
        data2[2]=6;
        ds.addTaskData("taskId2",data2);
        zmqr = new ZMQRequest("localhost", conf.getListeningPort(), conf);
        request = new DataRequest(zmqr,"taskId2",null,null,"msgpack");
        result =   request.send();
        request.close();
        assert ((byte[])result)[1] == 5;
        assert ((byte[])result)[2] == 6;
        zmqr = new ZMQRequest("localhost", conf.getListeningPort(), conf);
        request = new DataRequest(zmqr,"taskId1",null,null,"msgpack");
        result= request.send();
        assert ((JSONObject)result).query("/level1/value1").equals("d2_l1_value_1");
    }

    public JSONObject parseJSON(String filename) throws JSONException, IOException, URISyntaxException {
        String content = new String(Files.readAllBytes(Paths.get(getClass().getClassLoader()
                .getResource(filename)
                .toURI())));
        return new JSONObject(content);
    }


}

