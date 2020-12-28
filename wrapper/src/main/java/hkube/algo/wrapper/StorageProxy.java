package hkube.algo.wrapper;

import hkube.caching.DecodedCache;
import hkube.caching.EncodedCache;
import hkube.model.ObjectAndSize;
import hkube.storage.TaskStorage;
import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.lang3.StringUtils;

import java.io.FileNotFoundException;
import java.util.*;

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
            if (path.endsWith("result.json")) {
                ((List) result).forEach(resultPartReference -> {
                            Map info = (Map) ((Map) resultPartReference).get("info");
                            if (info != null) {
                                String message = (String) (info).get("message");
                                if (message != null && message.contains("too large")) {
                                    String partPath = (String) ((Map) info).get("path");
                                    try {
                                        ObjectAndSize resultPart = storage.getByFullPath(partPath);
                                        ((Map)resultPartReference).put("result", resultPart.getValue());
                                    } catch (FileNotFoundException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        }
                );
            }
            decodedCache.put(path, objectAndSize.getValue(), objectAndSize.getSize());
        }
        return result;
    }

    public boolean existsInCache(Map storageInfo) {
        String storageFullPath = (String) storageInfo.get("path");
        Object result = decodedCache.get(storageFullPath);
        return result != null;
    }

    public boolean allExistInCache(String jobId, List<String> tasks) {
        return tasks.stream().allMatch(taskId -> {
            String path = storage.createPath(jobId, taskId);
            Object result = decodedCache.get(path);
            return result != null;
        });
    }


    public Object get(String jobId, String taskId) throws FileNotFoundException {
        String path = storage.createPath(jobId, taskId);
        Object result = decodedCache.get(path);
        if (result == null) {
            ObjectAndSize objectAndSize = storage.get(jobId, taskId);
            decodedCache.put(path, objectAndSize.getValue(), objectAndSize.getSize());
        }
        return result;
    }

    public void setToCache(String jobId, String taskId, Object value, Integer size) {
        String path = storage.createPath(jobId, taskId);
        setToCache(path, value, size);
    }

    public void setToCache(String path, Object value, Integer size) {
        decodedCache.put(path, value, size);
    }

    public void setToCache(Map storageInfo, Object value, Integer size) {
        String storageFullPath = (String) storageInfo.get("path");
        setToCache(storageFullPath, value, size);
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
                    nextToken = "[" + (Integer.valueOf(nextToken) + 1) + "]";
                    relativePath = relativePath + nextToken;
                } else {
                    relativePath = relativePath + "/" + nextToken;
                }
            }
            if ((storedData instanceof Map || storedData instanceof Collection) && relativePath.length() > 0) {
                if (relativePath.startsWith("[")) {
                    relativePath = "." + relativePath;
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