package hkube.communication;

import hkube.encoding.EncodingManager;
import hkube.encoding.IEncoder;
import hkube.encoding.MSGPackEncoder;
import org.json.JSONObject;

import java.util.*;
import java.util.stream.Collectors;

public class DataServer implements IRequestListener {

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
        Map requestInfo = (Map) encoder.decode(request);
        String taskId = (String) requestInfo.get("taskId");
        String path = (String) requestInfo.get("path");
        List<String> tasks = (List) requestInfo.get("tasks");
        if (taskId == null && tasks == null) {
            communication.reply(this.encoder.encode(createError("unknown", "Request must contain either task or tasks attribute")));
        } else if (taskId != null) {
            communication.reply(this.encoder.encode(getResult(taskId, path)));
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
                if (data instanceof JSONObject) {
                    result = ((JSONObject) data).query("/" + path.replaceAll("\\.", "/"));
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
