package hkube.communication;

import hkube.encoding.EncodingManager;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.TimeoutException;

public abstract class DataRequest {
    IRequest requestAdapter;
    String taskId;
    List tasks;
    EncodingManager encoder;
    private static final Logger logger = LogManager.getLogger();

    public DataRequest(IRequest requestAdapter, List tasks, String encoding) {
        encoder = new EncodingManager(encoding);
        this.requestAdapter = requestAdapter;
        this.tasks = tasks;

        if (taskId != null && tasks != null) {
            logger.warn("DataRequest should contain either task or tasks, not both");
        }
    }

    public abstract Object send() throws TimeoutException;


    void close() {
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
        } else {
            return object;
        }
    }
}
