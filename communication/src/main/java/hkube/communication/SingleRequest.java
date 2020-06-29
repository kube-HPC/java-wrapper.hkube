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

public class SingleRequest extends DataRequest {
    public SingleRequest(IRequest requestAdapter, String taskId,  String path, String encoding) {
        super(requestAdapter, taskId, null, path, encoding);
    }
    private static final Logger logger = LogManager.getLogger();
    public Object send() throws TimeoutException {
        HashMap map = new HashMap();
        if (taskId != null) {
            map.put("taskId", taskId);
        }
        if (path != null) {
            map.put("dataPath", path);
        }
        if (tasks != null) {
            map.put("tasks", tasks);
        }
        Object decoded = encoder.decode(requestAdapter.send(encoder.encodeNoHeader(map)));
        Object result = toJSON(decoded);
        if (logger.isDebugEnabled()) {
            logger.debug(result);
        }
        if(result instanceof JSONObject){
            JSONObject jsonObjectResult = (JSONObject) result;
            if (jsonObjectResult.has("hkube_error")) {
                logger.warn(result.toString());
                throw new RuntimeException(jsonObjectResult.toString());
            }
            if (tasks != null ){
                if(jsonObjectResult.has("errors") && jsonObjectResult.getString("errors").equalsIgnoreCase("true")){
                    if(jsonObjectResult.has("items")){
                        JSONArray items = jsonObjectResult.getJSONArray("items");
                        Iterator itemIterator = items.iterator();
                        Map listResult = new HashMap<>();
                        int i=0;
                        while(itemIterator.hasNext()){
                            Object currentItem = itemIterator.next();
                            if(currentItem instanceof JSONObject && !((JSONObject) currentItem).has("hkube_error")){
                                listResult.put(tasks.get(i),currentItem);
                            }
                        }
                        result = listResult;
                    }
                }
            }
        }
        return result;
    }
}
