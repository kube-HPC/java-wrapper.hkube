package hkube.algo.wrapper;


import hkube.encoding.EncodingManager;
import hkube.encoding.IEncoder;
import hkube.storage.StorageFactory;
import hkube.storage.TaskStorage;

import org.apache.commons.lang3.StringUtils;
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


    public JSONArray placeData(JSONObject args) {
        Boolean useCache = args.getBoolean("useCache");
        if(!useCache){
            storageProxy.clear();
        }
        JSONObject storage = (JSONObject) args.get("storage");
        Object flatInput = args.get("flatInput");
        Map<String, Object> results = new HashMap<>();
        if (flatInput instanceof JSONObject && !((JSONObject) flatInput).isEmpty()) {
            Iterator<Map.Entry<String, Object>> iterator = ((JSONObject) flatInput).toMap().entrySet().iterator();

            while (iterator.hasNext()) {
                Object value = null;
                Map.Entry<String, Object> entry = iterator.next();
                Object dataReference = entry.getValue();
                if (!(dataReference instanceof String) ||!((String)dataReference).startsWith("$$")) {
                    value = dataReference;
                    results.put(entry.getKey(), value);
                } else {
                    dataReference = ((String)dataReference).substring(2);
                    Object item = storage.get((String) dataReference);
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
                                DataRequest request = new DataRequest(zmqr, null, tasks, path, config.commConfig.getEncodingType());
                                try {
                                    value = request.send();
                                } catch (Throwable e) {
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
                            DataRequest request = new DataRequest(zmqr, (String) single.get("taskId"), null, path, config.commConfig.getEncodingType());
                            try {
                                value = request.send();
                            } catch (TimeoutException e) {
                                logger.warn("Timeout trying to get output from " + host + ":" + port);
                            } catch (Throwable e) {
                                logger.warn("Exception getting data from peer : " + e.getMessage());
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
            return new JSONArray( results.values()) ;
        }
        return (JSONArray) args.get("input");
    }

    Map getMetadata(JSONArray savePaths, JSONObject result){
        Iterator<Object> pathsIterator = savePaths.iterator();
        Map metadata = new HashMap();
        while (pathsIterator.hasNext()) {
            String path = (String) pathsIterator.next();
            String nodeName = new StringTokenizer(path, ".").nextToken();
            String relativePath = path.replaceFirst(nodeName , "");
            relativePath =  relativePath.replaceAll("\\.", "/");
            Object value = result.query(relativePath);

            String type;
            JSONObject meta = new JSONObject();
            if (value instanceof Integer || value instanceof Long || value instanceof Double) {
                type = "number";
            }else if (value instanceof String) {
                type = "string";
            }else if (value instanceof JSONArray) {
                type = "array";
                meta.put("size",(((JSONArray) value).length()));
            }
            else{
                type="object";
            }

            meta.put("type",type);
            metadata.put(path,meta);
        }
        return metadata;
    }

    int  getEncodedSize(JSONObject toBeEncoded,String encodingType){
        byte[] encodedBytes =  new EncodingManager(encodingType).encode(toBeEncoded.toMap());
        return encodedBytes.length;
    }

    JSONObject wrapResult(WrapperConfig config, String jobId, String taskId,Map metadata,int size) {
        JSONObject wrappedResult = new JSONObject();

        JSONObject storageInfo = new JSONObject();
        String fullPath = new StorageFactory(config.storageConfig).getTaskStorage().createFullPath( jobId,taskId);
        storageInfo.put("path", fullPath);
        storageInfo.put("size",size);
        wrappedResult.put("storageInfo", storageInfo);

        JSONObject discoveryComm = new JSONObject();
        discoveryComm.put("host", config.commConfig.getListeningHost());
        discoveryComm.put("port", config.commConfig.getListeningPort());
        wrappedResult.put("discovery", discoveryComm);

        wrappedResult.put("taskId", taskId);
        wrappedResult.put("metadata",metadata);

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
