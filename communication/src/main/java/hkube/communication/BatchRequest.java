package hkube.communication;

import hkube.model.HeaderContentPair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.TimeoutException;

public class BatchRequest extends DataRequest {
    private static final Logger logger = LogManager.getLogger();

    public BatchRequest(IRequest requestAdapter, List tasks, String encoding) {
        super(requestAdapter, tasks, encoding);
    }

    public Map<String, Object> send() throws TimeoutException {
        logger.info("Invoking batch from peer");
        HashMap map = new HashMap();
        if (tasks != null) {
            map.put("tasks", tasks);
        }
        List<HeaderContentPair> reply = requestAdapter.send(encoder.encodeNoHeader(map));
        final List encoded = new ArrayList<>();
        reply.stream().forEach(rep -> {
            encoded.add(encoder.decodeSeparately(rep.getHeader(), rep.getContent()));
        });
        if (logger.isDebugEnabled()) {
            Object result = toJSON(encoded);
            logger.debug(result);
        }
        Map<String, Object> reslutMap = new HashMap<>();
        Iterator itemIterator = encoded.iterator();
        int i = 0;
        while (itemIterator.hasNext()) {
            Object currentItem = itemIterator.next();
            if (currentItem instanceof Map && (((Map) currentItem).get("hkube_error") == null)) {
                reslutMap.put((String) tasks.get(i), currentItem);
                i++;
            }
        }
        return reslutMap;
    }
}
