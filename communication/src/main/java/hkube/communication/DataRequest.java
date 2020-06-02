package hkube.communication;

import hkube.encoding.GeneralDecoder;
import hkube.encoding.MSGPackEncoder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeoutException;

public class DataRequest {
    IRequest requestAdapter;
    String taskId;
    List tasks;
    String path;
    GeneralDecoder decoder = new GeneralDecoder();
    MSGPackEncoder encoder = new MSGPackEncoder();


    public DataRequest(IRequest requestAdapter, String taskId, List tasks, String path) {
        this.requestAdapter = requestAdapter;
        this.taskId = taskId;
        this.tasks = tasks;
        this.path = path;
    }

    public Object send() throws TimeoutException {
        HashMap map = new HashMap() ;
        if(taskId != null){
            map.put("taskId",taskId);
        }
        if(path != null){
            map.put("path",path);
        }
        if(tasks != null){
            map.put("tasks",tasks);
        }
        Object decoded =  decoder.decode(requestAdapter.send(encoder.encodeNoHeader(map)));
        return toJSON(decoded);
    }
    void close(){
        requestAdapter.close();
    }
    public Object toJSON(Object object) throws JSONException {
        if (object instanceof HashMap) {
            JSONObject json = new JSONObject();
            HashMap map = (HashMap) object;
            for (Object key : map.keySet()) {
                json.put(key.toString(), toJSON(map.get(key)));
            }
            return json;
        } else if (object instanceof Iterable) {
            JSONArray json = new JSONArray();
            for (Object value : ((Iterable) object)) {
                json.put(toJSON(value));
            }
            return json;
        }
        else {
            return object;
        }
    }
}
