package hkube.algo.wrapper;

import hkube.storage.TaskStorage;
import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

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

    public Object getInputParamFromStorage(Map storageInfo, String path) {
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

    public Object getInputParamFromStorage(String jobId, String taskId, String path) {
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

    public void clear() {
        cache = new HashMap();
    }
}