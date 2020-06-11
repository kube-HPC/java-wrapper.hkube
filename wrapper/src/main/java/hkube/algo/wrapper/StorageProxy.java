package hkube.algo.wrapper;

import hkube.storage.TaskStorage;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

public class StorageProxy {
    HashMap cache = new HashMap();
    TaskStorage storage;

    StorageProxy(TaskStorage storage) {
        this.storage = storage;
    }

    public Object get(String path) throws FileNotFoundException {
        Object result = cache.get(path);
        if (result == null) {
            result = storage.getByFullPath(path);
            cache.put(path, result);
        }
        return result;
    }

    public Object get(String jobId, String taskId) throws FileNotFoundException {
        String path = storage.createPath(jobId, taskId);
        Object result = cache.get(path);
        if (result == null) {
            result = storage.get(jobId, taskId);
            cache.put(path, result);
        }
        return result;
    }
    public Object getInputParamFromStorage(JSONObject storageInfo, String path) {
        String storageFullPath = (String) storageInfo.get("path");
        Object value;
        try {
            Object storedData = get(storageFullPath);
            value = getSpecificData(storedData, path);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            value = null;
        }
        return value;
    }

   public  Object getInputParamFromStorage(String jobId, String taskId, String path) {
        Object value;
        try {
            Object storedData = get(jobId, taskId);
            value = getSpecificData(storedData, path);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            value = null;
        }
        return value;
    }

    private Object getSpecificData(Object storedData, String path) {
        Object value;
        if (storedData instanceof Map) {
            JSONObject storedDataJson = new JSONObject((Map) storedData);
            if (path.length() > 0) {
                String[] strArray = path.split("\\.");
                String lastToken = strArray[strArray.length - 1];
                Integer index = null;
                if (StringUtils.isNumeric(lastToken)) {
                    index = Integer.valueOf(lastToken);
                    path = path.replace("." + lastToken, "");
                }
                value = storedDataJson.query("/" + path.replaceAll("\\.", "/"));
                if (index != null) {
                    value = ((JSONArray) value).get(index);
                }
            } else {
                value = storedDataJson;
            }
        } else {
            value = storedData;
        }
        return value;
    }

    public void clear() {
        cache = new HashMap();
    }
}