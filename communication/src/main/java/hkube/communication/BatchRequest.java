package hkube.communication;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;
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

        if (logger.isDebugEnabled()) {
            JSONObject result = (JSONObject) toJSON(decoded);
            logger.debug(result);
        }
        Map reslutMap = new HashMap<>();

        if (decoded instanceof Map) {
            Map result = (Map) decoded;
            if (result.get("hkube_error") != null) {
                logger.warn(result.toString());
                throw new RuntimeException(result.toString());
            }

            if (result.get("items")!=null) {
                Collection items = (Collection) result.get("items");
                Iterator itemIterator = items.iterator();
                int i = 0;
                while (itemIterator.hasNext()) {
                    Object currentItem = itemIterator.next();
                    if (currentItem instanceof Map && (((Map)currentItem).get("hkube_error")==null)) {
                        reslutMap.put(tasks.get(i), currentItem);
                    }
                }
            }

        }
        return reslutMap;
    }
}
