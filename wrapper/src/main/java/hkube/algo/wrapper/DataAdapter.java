package hkube.algo.wrapper;


import hkube.encoding.GeneralDecoder;
import hkube.storage.StorageFactory;
import hkube.storage.TaskStorage;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import hkube.communication.zmq.ZMQRequest;
import hkube.communication.DataRequest;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.concurrent.TimeoutException;

public class DataAdapter {
    WrapperConfig config;

    TaskStorage taskStorage;
    StorageProxy storageProxy;

    public DataAdapter(WrapperConfig config) {
        this.config = config;
        taskStorage = new StorageFactory(config.storageConfig).getTaskStorage();
        storageProxy = new StorageProxy(taskStorage);
    }

    GeneralDecoder decoder = new GeneralDecoder();

    class StorageProxy {
        HashMap cache = new HashMap();
        TaskStorage storage;

        StorageProxy(TaskStorage storage) {
            this.storage = storage;
        }

        Object get(String path) throws FileNotFoundException {
            Object result = cache.get(path);
            if (result == null) {
                result = storage.getByFullPath(path);
                cache.put(path, result);
            }
            return result;
        }
    }

    public void placeData(JSONObject args) {
        JSONObject storage = (JSONObject) args.get("storage");
        Object flatInput = args.get("flatInput");

        if (flatInput instanceof JSONObject) {
            Iterator<Map.Entry<String, Object>> iterator = ((JSONObject) flatInput).toMap().entrySet().iterator();
            Map results = new HashMap<>();
            while (iterator.hasNext()) {
                Object value = null;
                Map.Entry entry = iterator.next();
                String dataReference = (String) entry.getValue();
                if (!dataReference.startsWith("$$")) {
                    value = dataReference;
                    results.put(entry.getKey(), value);
                } else {
                    dataReference = dataReference.substring(2);
                    Object item = storage.get(dataReference);
                    if (item instanceof JSONArray) {
                        value = new ArrayList();
                        Iterator batchIterator = ((JSONArray) item).iterator();
                        while (batchIterator.hasNext()) {
                            JSONObject single = (JSONObject) batchIterator.next();
                            String path = (String) single.get("path");
                            JSONObject discovery = (JSONObject) single.get("discovery");
                            JSONObject storageInfo = (JSONObject) single.get("storageInfo");
                            ((List) value).add(getInputParamFromStorage(storageInfo, path));
                        }
                    } else {
                        JSONObject single = (JSONObject) item;
                        String path = (String) single.get("path");
                        if (single.has("discovery")) {
                            JSONObject discovery = (JSONObject) single.get("discovery");
                            String host = (String) discovery.get("host");
                            String port = (String) discovery.get("port");
                            ZMQRequest zmqr = new ZMQRequest(host, port, config.commConfig);
                            DataRequest request = new DataRequest(zmqr, (String) single.get("taskId"), null, path);

                            try {
                                value = request.send();
                            } catch (TimeoutException e) {

                            }
                        }
                        if (value == null) {
                            JSONObject storageInfo = (JSONObject) single.get("storageInfo");
                            value = getInputParamFromStorage(storageInfo, path);
                        }
                    }
                    results.put(entry.getKey(), value);
                }
            }
            args.put("input", results);
        }
    }

    Object getInputParamFromStorage(JSONObject storageInfo, String path) {
        String storageFullPath = (String) storageInfo.get("path");
        Object value;
        try {
            Object storedData = storageProxy.get(storageFullPath);
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
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            value = null;
        }
        return value;
    }
}
