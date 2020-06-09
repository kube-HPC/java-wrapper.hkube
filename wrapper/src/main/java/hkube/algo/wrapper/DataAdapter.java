package hkube.algo.wrapper;


import hkube.storage.StorageFactory;
import hkube.storage.TaskStorage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import hkube.communication.zmq.ZMQRequest;
import hkube.communication.DataRequest;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class DataAdapter {
    WrapperConfig config;
    TaskStorage taskStorage;
    StorageProxy storageProxy;
    private static final Logger logger = LogManager.getLogger();
    public DataAdapter(WrapperConfig config) {
        this.config = config;
        taskStorage = new StorageFactory(config.storageConfig).getTaskStorage();
        storageProxy = new StorageProxy(taskStorage);
    }


    public void placeData(JSONObject args) {
        JSONObject storage = (JSONObject) args.get("storage");
        Object flatInput = args.get("flatInput");

        if (flatInput instanceof JSONObject) {
            Iterator<Map.Entry<String, Object>> iterator = ((JSONObject) flatInput).toMap().entrySet().iterator();
            Map<String, Object> results = new HashMap<>();
            while (iterator.hasNext()) {
                Object value = null;
                Map.Entry<String,Object> entry = iterator.next();
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
                            if (single.has("storageInfo")) {
                                JSONObject storageInfo = (JSONObject) single.get("storageInfo");
                                ((List) value).add(storageProxy.getInputParamFromStorage(storageInfo, path));
                            } else {
                                String host = (String) discovery.get("host");
                                String port = (String) discovery.get("port");
                                ZMQRequest zmqr = new ZMQRequest(host, port, config.commConfig);
                                List<String> tasks = getStringListFromJSONArray((JSONArray) single.get("tasks"));
                                DataRequest request = new DataRequest(zmqr, null, tasks, path);
                                try {
                                    value = request.send();
                                } catch (TimeoutException e) {
                                    String jobId = (String) args.get("jobId");
                                    value = tasks.stream().map((task) -> storageProxy.getInputParamFromStorage(jobId, task, path)).collect(Collectors.toList());
                                }
                            }
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
                                logger.warn("Timeout trying to get output from " + host+":"+port);
                            }
                        }
                        if (value == null) {
                            JSONObject storageInfo = (JSONObject) single.get("storageInfo");
                            value = storageProxy.getInputParamFromStorage(storageInfo, path);
                        }
                    }
                    results.put(entry.getKey(), value);
                }
            }
            args.put("input", results);
        }
    }
    JSONObject wrapResult(WrapperConfig config,String jobId, String taskId){
        JSONObject wrappedResult = new JSONObject();
        JSONObject storageInfo = new JSONObject();
        String fullPath = new StorageFactory(config.storageConfig).getTaskStorage().createFullPath(taskId,jobId);
        storageInfo.put("path",fullPath);
        storageInfo.put("metadata",new HashMap());
        JSONObject discoveryComm = new JSONObject();
        discoveryComm.put("host",config.commConfig.getListeningHost());
        discoveryComm.put("port", config.commConfig.getListeningPort());
        wrappedResult.put("discovery",discoveryComm);
        wrappedResult.put("storageInfo",storageInfo);
        wrappedResult.put("taskId",taskId);
        return wrappedResult;
    }

    public static List<String> getStringListFromJSONArray(JSONArray array) {
        ArrayList<String> jsonObjects = new ArrayList<>();
        for (int i = 0;
             i < (array != null ? array.length() : 0);
             jsonObjects.add(array.getString(i++))
        )
            ;
        return jsonObjects;
    }
}
