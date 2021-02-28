package hkube.communication;

import hkube.model.HeaderContentPair;
import hkube.model.ObjectAndSize;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.TimeoutException;

public class BatchRequest extends DataRequest {
    private static final Logger logger = LogManager.getLogger();

    public BatchRequest(IRequest requestAdapter, List tasks, String encoding) {
        super(requestAdapter, tasks, encoding);
    }

    public Map<String, ObjectAndSize> send() throws TimeoutException {
        logger.info("Invoking batch from peer");
        HashMap map = new HashMap();
        if (tasks != null) {
            map.put("tasks", tasks);
        }
        List<HeaderContentPair> reply = requestAdapter.send(encoder.encodeNoHeader(map));
        final List<ObjectAndSize> decodedList = new ArrayList<>();
        reply.stream().forEach(rep -> {
            Object decoded = encoder.decodeSeparately(rep.getHeader(), rep.getContent());
            ObjectAndSize objectAndSize = new ObjectAndSize(decoded, rep.getContent().length);
            decodedList.add(objectAndSize);
        });
        if (logger.isDebugEnabled()) {
            Object result = toJSON(decodedList);
            logger.debug(result);
        }
        Map<String, ObjectAndSize> reslutMap = new HashMap<>();
        Iterator<ObjectAndSize> itemIterator = decodedList.iterator();
        int i = 0;
        while (itemIterator.hasNext()) {
            ObjectAndSize currentItem = itemIterator.next();
            Object currentValue = currentItem.getValue();

            if (! (currentValue instanceof Map && (((Map) currentValue).get("hkube_error") != null))) {
                reslutMap.put((String) tasks.get(i), currentItem);
                i++;
            }
        }
        return reslutMap;
    }
}
