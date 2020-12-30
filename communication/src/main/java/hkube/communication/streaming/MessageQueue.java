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
    Double maxSize;

    private class ConsumerStats {
        int index = 0;
        int dropped = 0;
        int added = 0;
        int sent = 0;
    }

    public MessageQueue(String name, List<String> consumers, Double maxSize) {
        sourceName = name;
        this.maxSize = maxSize;
        consumers.stream().forEach(consumer -> {
            consumersMap.put(consumer, new ConsumerStats());
        });
    }

    public void push(Message message) {
        memorySize += message.data.length;
        while(maxSize<memorySize){
            looseMessage();
        }
        consumersMap.values().stream().forEach(stats -> {
            stats.added += 1;
        });
        queue.add(message);
    }

    private void looseMessage() {
        Message msg = queue.remove(0);
        memorySize -= msg.data.length;
        consumersMap.values().stream().forEach(currentStats -> {
            if (currentStats.index == 0) {
                currentStats.dropped++;
            }else {
                currentStats.index--;
            }
        });

    }

    public Message pop(String consumer) {
        int nextIndex = nextMessageIndex(consumer);
        ConsumerStats stats = consumersMap.get(consumer);
        if (nextIndex >= 0) {
            Message msg = queue.get(nextIndex);
            stats.index = nextIndex + 1;
            if (!consumersMap.values().stream().anyMatch(currentStats -> currentStats.index == 0)) {
                queue.remove(0);
                memorySize -= msg.data.length;
                consumersMap.values().stream().forEach(currentStats -> {
                    currentStats.index--;
                });
            }
            stats.sent += 1;
            return msg;
        }
        return null;
    }


    private int nextMessageIndex(String consumer) {
        int i = consumersMap.get(consumer).index;
        while (i < queue.size()) {
            if (queue.get(i).flow.isNextInFlow(sourceName, consumer)) {
                return i;
            }
            i++;
        }
        return -1;
    }

    public int getInQueue(String consumer) {
        ConsumerStats stats = consumersMap.get(consumer);
        return stats.added-stats.sent;
    }
    public int getSent(String consumer) {
        ConsumerStats stats = consumersMap.get(consumer);
        return stats.sent;
    }
    public int getDropped(String consumer) {
        ConsumerStats stats = consumersMap.get(consumer);
        return stats.dropped;
    }
}
