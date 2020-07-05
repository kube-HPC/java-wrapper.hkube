package hkube.communication;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.util.*;
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

        if (logger.isDebugEnabled()) {
            Object result = toJSON(decoded);
            logger.debug(result);
        }
        if(decoded instanceof Map){
            Map result = (Map)decoded;
                if (result.get("hkube_error")!=null) {
                logger.warn(result.toString());
                throw new RuntimeException(result.toString());
            }
            if (tasks != null ){
                if(result.get("errors")!=null && (Boolean) result.get("errors")==true){
                    if(result.get("items")!=null){
                        Collection items = (Collection)result.get("items");
                        Iterator itemIterator = items.iterator();
                        Map listResult = new HashMap<>();
                        int i=0;
                       while(itemIterator.hasNext()){
                            Object currentItem = itemIterator.next();
                            if(currentItem instanceof JSONObject && !((JSONObject) currentItem).has("hkube_error")){
                                listResult.put(tasks.get(i),currentItem);
                            }
                        }
                        decoded = listResult;
                    }
                }
            }
        }
        return decoded;
    }
}
