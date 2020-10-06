package hkube.algo.wrapper;

import hkube.caching.DecodedCache;
import hkube.caching.EncodedCache;
import hkube.model.ObjectAndSize;
import hkube.storage.TaskStorage;
import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.lang3.StringUtils;

import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

public class StorageProxy {
    DecodedCache decodedCache = new DecodedCache();
    TaskStorage storage;

    StorageProxy(TaskStorage storage) {
        this.storage = storage;
    }

    public Object get(String path) throws FileNotFoundException {
        Object result = decodedCache.get(path);
        if (result == null) {
            ObjectAndSize objectAndSize = storage.getByFullPath(path);
            result = objectAndSize.getValue();
            decodedCache.put(path, objectAndSize.getValue(),objectAndSize.getSize());
        }
        return result;
    }

    public Object get(String jobId, String taskId) throws FileNotFoundException {
        String path = storage.createPath(jobId, taskId);
        Object result = decodedCache.get(path);
        if (result == null) {
            ObjectAndSize objectAndSize  = storage.get(jobId, taskId);
            decodedCache.put(path, objectAndSize.getValue(),objectAndSize.getSize());
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

    public Object getSpecificData(Object storedData, String path) {
        Object value;
        if (path != null && path.length() > 0) {
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
}