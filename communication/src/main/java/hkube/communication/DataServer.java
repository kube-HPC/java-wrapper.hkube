package hkube.communication;


import hkube.caching.EncodedCache;
import hkube.encoding.EncodingManager;
import hkube.encoding.IEncoder;
import hkube.model.HeaderContentPair;
import org.apache.commons.jxpath.JXPathContext;
import org.json.JSONObject;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DataServer implements IRequestListener {
    private static final Logger logger = LogManager.getLogger();
    IRequestServer communication;
    IEncoder encoder;
    HeaderContentPair notAvailableError;


    EncodedCache dataCache = new EncodedCache();
    ICommConfig conf;

    public DataServer(IRequestServer communication, ICommConfig conf) {
        communication.addRequestsListener(this);
        encoder = new EncodingManager(conf.getEncodingType());
        notAvailableError = encoder.encodeSeparately(createError("notAvailable", "taskId notAvailable").toMap());
        this.communication = communication;
        this.conf = conf;
    }

    public boolean addTaskData(String taskId, HeaderContentPair data) {
        return this.dataCache.put(taskId, data, data.getContent().length) == null;
    }

    @Override
    public void onRequest(byte[] request) {
        try {
            logger.debug("Got Request");
            Map requestInfo = (Map) encoder.decodeNoHeader(request);
            if (logger.isDebugEnabled()) {
                logger.debug("Got request " + new JSONObject((requestInfo)));
            }

            List<String> tasks = (List) requestInfo.get("tasks");

            if (tasks == null) {
                HeaderContentPair encodedError = this.encoder.encodeSeparately(createError("unknown", "Request must contain either task or tasks attribute"));
                List<HeaderContentPair> reply = new ArrayList();
                reply.add(new HeaderContentPair(encodedError.getHeaderAsBytes(), encodedError.getContent()));
                communication.reply(reply);
            } else {
                List<HeaderContentPair> items = tasks.stream().map((task) -> getResult(task)).collect(Collectors.toList());
                List<HeaderContentPair> reply = items.stream().map(item ->
                        new HeaderContentPair(item.getHeaderAsBytes(), item.getContent())
                ).collect(Collectors.toList());
                if (logger.isDebugEnabled()) {
                    logger.debug("Responding " + reply);
                }
                communication.reply(reply);
            }
        } catch (Throwable e) {
            List<HeaderContentPair> reply = new ArrayList();
            HeaderContentPair encodedError = encoder.encodeSeparately(createError("unknown", "Unexpected error " + e.getMessage()));
            reply.add(new HeaderContentPair(encodedError.getHeaderAsBytes(), encodedError.getContent()));
            logger.warn("Data server responding:" + reply);
            communication.reply(reply);
        }
    }

    private HeaderContentPair getResult(String taskId) {
        HeaderContentPair data = dataCache.get(taskId);
        if (data == null) {
            data = notAvailableError;
        }
        return data;
    }

    private Object getSpecificData(Object storedData, String path) {
        Object value;
        if (path.length() > 0) {
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
