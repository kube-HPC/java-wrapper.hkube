package hkube.communication.streaming;

import java.util.*;

public class MessageQueue {
    String sourceName;
    Map<String, ConsumerStats> consumersMap = new HashMap();
    Map<String, ArrayDeque<Long>> queueDurationCache = new HashMap();
    List<Message> queue = new ArrayList();

    public int getMemorySize() {
        return memorySize;
    }

    public Double getMaxSize() {
        return maxSize;
    }

    int memorySize = 0;
    Double maxSize;

    private class ConsumerStats {
        int index = 0;
        int dropped = 0;
        int added = 0;
        int sent = 0;
    }

    public boolean anyLeft() {
        return queue.size() > 0;
    }

    public MessageQueue(String name, List<String> consumers, Double maxSize) {
        sourceName = name;
        this.maxSize = maxSize;
        consumers.stream().forEach(consumer -> {
            consumersMap.put(consumer, new ConsumerStats());
            queueDurationCache.put(consumer, new ArrayDeque<>());
        });
    }

    synchronized public void push(Message message) {
        memorySize += message.data.length;
        while (maxSize < memorySize) {
            looseMessage();
        }
        consumersMap.keySet().stream().forEach(consumer -> {
            if (message.getFlow().isNextInFlow(sourceName, consumer)) {
                consumersMap.get(consumer).added += 1;
            }
        });
        queue.add(message);
    }

    synchronized private void looseMessage() {
        Message msg = queue.remove(0);
        memorySize -= msg.data.length;
        consumersMap.entrySet().stream().forEach(currentStats -> {
            if (currentStats.getValue().index == 0) {
                if (msg.getFlow().isNextInFlow(sourceName, currentStats.getKey())) {
                    currentStats.getValue().dropped++;
                }
            } else {
                currentStats.getValue().index--;
            }
        });

    }

    synchronized public Message pop(String consumer) {
        int nextIndex = nextMessageIndex(consumer);
        ConsumerStats stats = consumersMap.get(consumer);
        if (nextIndex >= 0) {
            Message msg = queue.get(nextIndex);
            stats.index = nextIndex + 1;
            while (removeIfNeeded()) {

            }
            stats.sent += 1;
            Long queueTime = System.currentTimeMillis() - msg.produceTime;
            synchronized (queueDurationCache) {
                queueDurationCache.get(consumer).add(queueTime);
            }
            return msg;
        }
        return null;
    }

    synchronized public boolean removeIfNeeded() {
        if (queue.size() > 0) {
            final Message msg = queue.get(0);
            if (!consumersMap.entrySet().stream().filter(currentStats -> currentStats.getValue().index == 0).anyMatch(consumerStats -> msg.getFlow().isNextInFlow(sourceName, consumerStats.getKey()))) {
                queue.remove(0);
                memorySize -= msg.data.length;
                consumersMap.values().stream().forEach(currentStats -> {
                    if (currentStats.index != 0) {
                        currentStats.index--;
                    }
                });
                return true;
            }
        }
        return false;
    }

    synchronized private int nextMessageIndex(String consumer) {
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
        return stats.added - stats.sent - stats.dropped;
    }

    public ArrayDeque resetQueueTimeDurations(String consumer) {
        ArrayDeque durations;
        synchronized (queueDurationCache) {

            durations = queueDurationCache.get(consumer);
            queueDurationCache.put(consumer, new ArrayDeque<>());
        }
        return durations;

    }

    public int getInQueue() {
        return queue.size();
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
