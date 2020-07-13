package hkube.algo.wrapper;


import hkube.communication.BatchRequest;
import hkube.communication.SingleRequest;
import hkube.encoding.EncodingManager;
import hkube.storage.StorageFactory;
import hkube.storage.TaskStorage;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.jxpath.JXPathContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import hkube.communication.zmq.ZMQRequest;

import java.nio.ByteBuffer;
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


    public Collection placeData(Map args) {
        Boolean useCache = (Boolean) args.get("useCache");
        if (!useCache) {
            storageProxy.clear();
        }
        Map storage = (Map) args.get("storage");
        Map<String, Object> results = new HashMap<>();
        Object flatInput = args.get("flatInput");
        if (flatInput instanceof Map && !((Map) flatInput).isEmpty()) {
            Iterator<Map.Entry<String, Object>> iterator = ((Map) flatInput).entrySet().iterator();
            while (iterator.hasNext()) {
                Object value;
                Map.Entry<String, Object> entry = iterator.next();
                Object dataReference = entry.getValue();
                if (!(dataReference instanceof String) || !((String) dataReference).startsWith("$$")) {
                    value = dataReference;
                    String key = entry.getKey();
                    String[] keyParts = key.split("\\.");
                    Map tempValue = results;
                    for (int i = 0; i < keyParts.length - 1; i++) {
                        Map partMap = (Map) tempValue.get(keyParts[i]);
                        if (partMap == null) {
                            tempValue.put(keyParts[i], new HashMap<>());
                        }
                        tempValue = (Map) tempValue.get(keyParts[i]);
                    }
                    tempValue.put(keyParts[keyParts.length - 1], value);
                } else {
                    dataReference = ((String) dataReference).substring(2);
                    Object item = storage.get((String) dataReference);
                    String jobId = (String) args.get("jobId");
                    if (item instanceof Collection) {
                        value = new ArrayList();
                        Iterator batchIterator = ((Collection) item).iterator();
                        while (batchIterator.hasNext()) {
                            Map single = (Map) batchIterator.next();
                            Object singleData = getData(single, jobId);
                            ((List) value).add(singleData);
                        }
                    } else {
                        value = getData((Map) item, jobId);
                    }
                    String key = entry.getKey();
                    String[] keyParts = key.split("\\.");
                    Map tempValue = results;
                    for (int i = 0; i < keyParts.length - 1; i++) {
                        Map partMap = (Map) tempValue.get(keyParts[i]);
                        if (partMap == null) {
                            tempValue.put(keyParts[i], new HashMap<>());
                        }
                        tempValue = (Map) tempValue.get(keyParts[i]);
                    }
                    tempValue.put(keyParts[keyParts.length - 1], value);
                }
            }
            ;
            return (Collection) getAsArray(results);
        }

        Collection originalInput = (Collection) args.get("input");
        Iterator iterator = originalInput.iterator();
        Collection inputList = new ArrayList();
        while (iterator.hasNext()) {
            Object value = iterator.next();
            if (value instanceof byte[]) {
                value = ByteBuffer.wrap((byte[]) value);
            }
            inputList.add(value);
        }
        return inputList;
    }

    private Object getAsArray(Map<String, Object> results) {
        results.entrySet().stream().forEach(entry -> {
            if (entry.getValue() instanceof Map) {
                results.put(entry.getKey(), getAsArray((Map) entry.getValue()));
            }
        });
        if (results.keySet().stream().anyMatch(key -> StringUtils.isNumeric(key))) {
            return results.values();
        }
        else{
            return results;
        }

    }

    public Object getData(Map single, String jobId) {
        Object value = null;
        final String path;
        if (single.get("path") != null) {
            path = (String) single.get("path");
        } else {
            path = "";
        }
        String task = null;
        List<String> tasks = null;


        if (single.get("discovery") != null) {

            Map discovery = (Map) single.get("discovery");
            String host = (String) discovery.get("host");
            String port = (String) discovery.get("port");
            ZMQRequest zmqr = new ZMQRequest(host, port, config.commConfig);
            SingleRequest singleRequest = null;
            BatchRequest batchRequest = null;
            if (single.get("tasks") != null) {
                //batch with discovery
                tasks = getStringListFromJSONArray((Collection) single.get("tasks"));
                batchRequest = new BatchRequest(zmqr, tasks, path, config.commConfig.getEncodingType());
            } else {
                task = (String) single.get("taskId");
                singleRequest = new SingleRequest(zmqr, task, path, config.commConfig.getEncodingType());
            }
            try {
                if (singleRequest != null)
                    value = singleRequest.send();
                else {
                    Map batchReslut = batchRequest.send();
                    List<String> missingTasks = tasks.stream().filter(taskId -> !batchReslut.containsKey(taskId)).collect(Collectors.toList());
                    value = missingTasks.stream().map((taskId) -> storageProxy.getInputParamFromStorage(jobId, taskId, path)).collect(Collectors.toList());
                    ((Collection) value).addAll(batchReslut.values());
                }
            } catch (TimeoutException e) {
                logger.warn("Timeout trying to get output from " + host + ":" + port);
            } catch (Throwable e) {
                logger.warn("Exception getting data from peer : " + e.getMessage());
            }
        }
        if (value == null) {
            if (single.get("storageInfo") != null) {
                Map storageInfo = (Map) single.get("storageInfo");
                value = storageProxy.getInputParamFromStorage(storageInfo, path);
            } else {
                //batch without discovery
                if (single.get("tasks") != null) {
                    tasks = getStringListFromJSONArray((Collection) single.get("tasks"));
                    value = tasks.stream().map((taskId) -> storageProxy.getInputParamFromStorage(jobId, taskId, path)).collect(Collectors.toList());
                }
            }
        }
        return value;
    }


    public static Map getMetadata(Collection savePaths, Object result) {
        Iterator<Object> pathsIterator = savePaths.iterator();
        Map metadata = new HashMap();
        while (pathsIterator.hasNext()) {
            String path = (String) pathsIterator.next();
            StringTokenizer tokenizer = new StringTokenizer(path, ".");
            String relativePath = "";
            String nodeName = tokenizer.nextToken();
            while (tokenizer.hasMoreElements()) {
                String nextToken = tokenizer.nextToken();
                if (StringUtils.isNumeric(nextToken)) {
                    nextToken = "[" + nextToken + "]";
                    relativePath = relativePath + nextToken;
                } else {
                    relativePath = relativePath + "/" + nextToken;
                }
            }
            try {
                Object value;
                if ((result instanceof Map || result instanceof Collection) && relativePath.length() > 0) {
                    value = JXPathContext.newContext(result).getValue(relativePath);
                } else {
                    value = result;
                }
                String type;
                Map meta = new HashMap();
                if (value instanceof Integer || value instanceof Long || value instanceof Double) {
                    type = "number";
                } else if (value instanceof String) {
                    type = "string";
                } else if (value instanceof byte[]) {
                    type = "bytearray";
                    meta.put("size", (((byte[]) value).length));
                } else if (value instanceof Collection) {
                    type = "array";
                    meta.put("size", (((Collection) value).size()));
                } else {
                    type = "object";
                }

                meta.put("type", type);
                metadata.put(path, meta);
            } catch (Throwable e) {
                logger.warn("Problem while getting meta data for " + relativePath);
                logger.error(e.getMessage());
                continue;
            }
        }
        return metadata;
    }

    byte[] encode(Object toBeEncoded, String encodingType) {
        byte[] encodedBytes = new EncodingManager(encodingType).encode(toBeEncoded);
        return encodedBytes;
    }

    Map getStoringInfo(WrapperConfig config, String jobId, String taskId, Map metadata, int size) {
        Map wrappedResult = new HashMap();

        Map storageInfo = new HashMap();
        String fullPath = new StorageFactory(config.storageConfig).getTaskStorage().createFullPath(jobId, taskId);
        storageInfo.put("path", fullPath);
        storageInfo.put("size", size);
        wrappedResult.put("storageInfo", storageInfo);

        Map discoveryComm = new HashMap();
        discoveryComm.put("host", config.commConfig.getListeningHost());
        discoveryComm.put("port", config.commConfig.getListeningPort());
        wrappedResult.put("discovery", discoveryComm);

        wrappedResult.put("taskId", taskId);
        wrappedResult.put("metadata", metadata);

        return wrappedResult;
    }

    public static List<String> getStringListFromJSONArray(Collection array) {
        ArrayList<String> jsonObjects = new ArrayList<>();
        Iterator iterator = array.iterator();
        while (iterator.hasNext())
            jsonObjects.add(iterator.next().toString());
        return jsonObjects;
    }
}
