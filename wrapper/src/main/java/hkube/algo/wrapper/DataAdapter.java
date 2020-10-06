package hkube.algo.wrapper;


import hkube.communication.BatchRequest;
import hkube.communication.SingleRequest;

import hkube.encoding.EncodingManager;
import hkube.model.HeaderContentPair;
import hkube.storage.StorageFactory;
import hkube.storage.TaskStorage;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.jxpath.JXPathContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import hkube.communication.zmq.ZMQRequest;

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
        Map storage = (Map) args.get("storage");
        Map<String, Object> results = new HashMap<>();
        Object flatInput = args.get("flatInput");
        if (flatInput instanceof Map && !((Map) flatInput).isEmpty()) {
            Iterator<Map.Entry<String, Object>> iterator = ((Map) flatInput).entrySet().iterator();
            while (iterator.hasNext()) {
                Object value;
                Map.Entry<String, Object> entry = iterator.next();
                String key = entry.getKey();
                Object dataReference = entry.getValue();
                if (!(dataReference instanceof String) || !((String) dataReference).startsWith("$$")) {
                    value = dataReference;
                } else {
                    dataReference = ((String) dataReference).substring(2);
                    Object item = storage.get(dataReference);
                    String jobId = (String) args.get("jobId");
                    if (item instanceof List) {
                        Map batchInfp = (Map) ((List) item).get(0);
                        value = getData(batchInfp, jobId);
                    } else {
                        value = getData((Map) item, jobId);
                    }
                }
                String[] keyParts = key.split("\\.");
                Object currentValue = args.get("input");
                Object tempValue = currentValue;
                for (int i = 0; i < keyParts.length - 1; i++) {
                    if (StringUtils.isNumeric(keyParts[i])) {
                        tempValue = ((List) tempValue).get(Integer.valueOf(keyParts[i]));

                    } else {
                        tempValue = ((Map) tempValue).get(keyParts[i]);
                    }
                }
                String index = keyParts[keyParts.length - 1];
                if (StringUtils.isNumeric(keyParts[keyParts.length - 1])) {
                    ((ArrayList) tempValue).set(Integer.valueOf(index), value);

                } else {
                    ((Map) tempValue).put(index, value);
                }
            }
        }

        Collection originalInput = (Collection) args.get("input");
        Iterator iterator = originalInput.iterator();
        Collection inputList = new ArrayList();
        while (iterator.hasNext()) {
            Object value = iterator.next();
            inputList.add(value);
        }
        return originalInput;
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
                batchRequest = new BatchRequest(zmqr, tasks, config.commConfig.getEncodingType());
            } else {
                task = (String) single.get("taskId");
                singleRequest = new SingleRequest(zmqr, task, config.commConfig.getEncodingType());
            }
            try {
                if (singleRequest != null) {
                    value = singleRequest.send();
                    value = storageProxy.getSpecificData(value, path);
                } else {
                    Map<String, Object> batchReslut = batchRequest.send();
                    List resultValues = batchReslut.values().stream().map(result -> {
                        if (path != null && !path.equals(""))
                            return storageProxy.getSpecificData(result, path);
                        else
                            return result;
                    }).collect(Collectors.toList());


                    List<String> missingTasks = tasks.stream().filter(taskId -> !batchReslut.containsKey(taskId)).collect(Collectors.toList());
                    logger.info("Got " + (tasks.size() - missingTasks.size()) + "valid task results from batch request");
                    value = missingTasks.stream().map((taskId) -> storageProxy.getInputParamFromStorage(jobId, taskId, path)).collect(Collectors.toList());
                    ((Collection) value).addAll(resultValues);
                }
            } catch (TimeoutException e) {
                logger.warn("Timeout trying to get output from " + host + ":" + port);
            } catch (Throwable e) {
                logger.warn("Exception getting data from peer : " + e.getMessage());
            }
        }
        if (value == null) {
            logger.info("value null getting from storage");
            if (single.get("storageInfo") != null) {
                Map storageInfo = (Map) single.get("storageInfo");
                logger.info("Getting single task result from storage");
                value = storageProxy.getInputParamFromStorage(storageInfo, path);
                logger.info("Got value from storage");
            } else {
                //batch without discovery
                if (single.get("tasks") != null) {
                    tasks = getStringListFromJSONArray((Collection) single.get("tasks"));
                    logger.info("Getting result of batch from storage");
                    value = tasks.stream().map((taskId) -> storageProxy.getInputParamFromStorage(jobId, taskId, path)).collect(Collectors.toList());
                    logger.info("Got value from storage");
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
                    nextToken = "[" + (Integer.valueOf(nextToken) + 1) + "]";
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

    HeaderContentPair encode(Object toBeEncoded, String encodingType) {
        HeaderContentPair encodedBytes = new EncodingManager(encodingType).encodeSeparately(toBeEncoded);
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
