package hkube.communication;


import hkube.encoding.EncodingManager;
import hkube.encoding.IEncoder;
import org.apache.commons.jxpath.JXPathContext;
import org.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
public class DataServer implements IRequestListener {
    private static final Logger logger = LogManager.getLogger();
    IRequestServer communication;
    IEncoder encoder;

    private class DataCache {
        Map<String, Object> dataByTaskIdMap = new HashMap();
        SortedMap<Long, String> tasksByTime = new TreeMap<Long, String>();

        private void removeOldestFromCache() {
            long oldestTime = tasksByTime.firstKey();
            String taskId = tasksByTime.get(oldestTime);
            tasksByTime.remove(oldestTime);
            dataByTaskIdMap.remove(taskId);
        }

        public void put(String taskId, Object data) {
            if (dataByTaskIdMap.size() == conf.getMaxCacheSize()) {
                removeOldestFromCache();
            }
            dataByTaskIdMap.put(taskId, data);
            tasksByTime.put(new Date().getTime(), taskId);
        }

        public Object getData(String taskId) {
            return dataByTaskIdMap.get(taskId);
        }

    }

    DataCache dataCache = new DataCache();
    ICommConfig conf;

    public DataServer(IRequestServer communication, ICommConfig conf) {
        communication.addRequestsListener(this);
        encoder = new EncodingManager(conf.getEncodingType());
        this.communication = communication;
        this.conf = conf;
    }

    public void addTaskData(String taskId, Object data) {
        this.dataCache.put(taskId, data);
    }

    @Override
    public void onRequest(byte[] request) {
        try {
            logger.debug("Got Request");
            Map requestInfo = (Map) encoder.decode(request);
            if(logger.isDebugEnabled()){
                logger.debug("Got request "+new JSONObject((requestInfo)));
            }
            String taskId = (String) requestInfo.get("taskId");
            String path = (String) requestInfo.get("dataPath");
            List<String> tasks = (List) requestInfo.get("tasks");

            if (taskId == null && tasks == null) {
                communication.reply(this.encoder.encode(createError("unknown", "Request must contain either task or tasks attribute")));
            } else if (taskId != null) {
                Object result = getResult(taskId,path);
                if(logger.isDebugEnabled()){
                    logger.debug("Responding" + result);
                }
                communication.reply(this.encoder.encode(result));
            } else {
                List items = tasks.stream().map((task) -> getResult(task, path)).collect(Collectors.toList());
                boolean hasError = items.stream().anyMatch(item -> {
                    if (item.getClass().equals(JSONObject.class) && ((JSONObject) item).get("hkube_error") != null)
                        return true;
                    else return false;
                });
                JSONObject result = new JSONObject();
                result.put("items", items);
                result.put("errors", hasError);
                if(logger.isDebugEnabled()){
                    logger.debug("Responding "+result);
                }
                communication.reply(this.encoder.encode(result.toMap()));
            }
        } catch (Throwable e) {
            JSONObject result = new JSONObject();
            List items = new ArrayList();
            items.add(createError("unknown", "Unexpected error " + e.getMessage()));
            result.put("items", items);
            result.put("errors", true);
            logger.warn("Data server responding:" + result);
            communication.reply(this.encoder.encode(result.toMap()));
        }
    }

    private Object getResult(String taskId, String path) {
        Object data = dataCache.getData(taskId);
        Object result;
        if (data == null) {
            result = createError("notAvailable", "taskId notAvailable").toMap();
        } else {

            if (path != null && !path.equals("")) {
                if (data instanceof Map || data instanceof Collection) {
                    if(logger.isDebugEnabled()){
                        logger.debug("quering " + path +" from " + data);
                    }
                    result = getSpecificData(data,path);
                } else {
                    result = createError("unknown", "Can't get data by path, data is not json");
                }
            } else {
                result = data;
            }
        }
        if (result instanceof JSONObject) {
            return ((JSONObject) result).toMap();
        } else {
            return result;
        }
    }

    private Object getSpecificData(Object storedData, String path) {
        Object value;
        if (path.length() > 0) {
            StringTokenizer tokenizer = new StringTokenizer(path, ".");
            String relativePath = "";
            while (tokenizer.hasMoreElements()) {
                String nextToken = tokenizer.nextToken();
                if (StringUtils.isNumeric(nextToken)) {
                    nextToken = "[" + (Integer.valueOf(nextToken)+1) + "]";
                    relativePath = relativePath + nextToken;
                } else {
                    relativePath = relativePath + "/" + nextToken;
                }
            }
            if ((storedData instanceof Map|| storedData instanceof Collection) && relativePath.length() > 0) {
                if (relativePath.startsWith("[")){
                    relativePath="."+relativePath;
                }
                value = JXPathContext.newContext(storedData).getValue(relativePath);
            } else {
                value = storedData;
            }
        } else {
            value = storedData;
        }
        return value;
    }

    private JSONObject createError(String code, String message) {
        JSONObject hkubeError = new JSONObject();
        hkubeError.put("code", code);
        hkubeError.put("message", message);
        JSONObject result = new JSONObject();
        result.put("hkube_error", hkubeError);
        return result;
    }

    public void close() {
        communication.close();
    }

}
