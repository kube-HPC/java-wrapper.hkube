package hkube.communication.streaming;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageQueue {
    String sourceName;
    Map<String, ConsumerStats> consumersMap = new HashMap();
    List<Message> queue = new ArrayList();
    int memorySize = 0;

    private class ConsumerStats {
        int index = 0;
        int removed = 0;
        int added = 0;
    }

    public MessageQueue(String name, List<String> consumers) {
        consumers.stream().forEach(consumer -> {
            consumersMap.put(consumer, new ConsumerStats());
        });
    }

    public void push(Message message) {
        memorySize += message.data.length;
        consumersMap.values().stream().forEach(stats -> {
            stats.index += 1;
        });
        queue.add(message);
    }

    public Message pop(String consumer) {
        int nextIndex = nextMessageIndex(consumer);
        ConsumerStats stats = consumersMap.get(consumer);
        if (nextIndex >= 0) {
            Message msg = queue.get(nextIndex);
            stats.index = nextIndex;
            if (!consumersMap.values().stream().anyMatch(currentStats -> currentStats.index == 0)) {
                queue.remove(0);
                memorySize -= msg.data.length;
                consumersMap.values().stream().forEach(currentStats -> {
                    currentStats.index--;
                });
            }
            return msg;
        }
        return null;
    }

    private int nextMessageIndex(String consumer) {
        int i = consumersMap.get(consumer).index;
        while (i < queue.size()) {
            if (queue.get(i - 1).flow.isNextInFlow(sourceName, consumer)) {
                return i;
            }
            i++;
        }
        return -1;
    }
}
