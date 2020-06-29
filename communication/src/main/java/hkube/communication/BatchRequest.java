package hkube.communication;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class BatchRequest extends DataRequest {
    private static final Logger logger = LogManager.getLogger();

    public BatchRequest(IRequest requestAdapter, List tasks, String path, String encoding) {
        super(requestAdapter, null, tasks, path, encoding);
    }

    public Map send() throws TimeoutException {
        HashMap map = new HashMap();
        if (path != null) {
            map.put("dataPath", path);
        }
        if (tasks != null) {
            map.put("tasks", tasks);
        }
        Object decoded = encoder.decode(requestAdapter.send(encoder.encodeNoHeader(map)));
        JSONObject result = (JSONObject) toJSON(decoded);
        if (logger.isDebugEnabled()) {
            logger.debug(result);
        }
        Map reslutMap = new HashMap<>();
        if (result instanceof JSONObject) {
            if (result.has("hkube_error")) {
                logger.warn(result.toString());
                throw new RuntimeException(result.toString());
            }
            if (result.has("errors") && result.getString("errors").equalsIgnoreCase("true")) {
                if (result.has("items")) {
                    JSONArray items = result.getJSONArray("items");
                    Iterator itemIterator = items.iterator();
                    int i = 0;
                    while (itemIterator.hasNext()) {
                        Object currentItem = itemIterator.next();
                        if (currentItem instanceof JSONObject && !((JSONObject) currentItem).has("hkube_error")) {
                            reslutMap.put(tasks.get(i), currentItem);
                        }
                    }

                }
            }
        }
        return reslutMap;
    }
}
