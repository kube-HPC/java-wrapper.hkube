package hkube.communication.streaming;

import java.util.ArrayDeque;

public class Statistics {
    String nodeName;
    Integer sent;
    Integer queueSize;
    Integer responses;
    ArrayDeque<Double> durations;
    Integer dropped;


    public Statistics(String nodeName, Integer sent, Integer queueSize, ArrayDeque<Double> durations, Integer responses, Integer dropped) {
        this.nodeName = nodeName;
        this.sent = sent;
        this.queueSize = queueSize;
        this.responses = responses;
        this.durations = durations;
        this.dropped = dropped;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public Integer getSent() {
        return sent;
    }

    public void setSent(Integer sent) {
        this.sent = sent;
    }

    public Integer getQueueSize() {
        return queueSize;
    }

    public void setQueueSize(Integer queueSize) {
        this.queueSize = queueSize;
    }

    public ArrayDeque<Double> getDurations() {
        return durations;
    }

    public void setDurations(ArrayDeque<Double> durations) {
        this.durations = durations;
    }

    public Integer getResponses() {
        return responses;
    }

    public void setResponses(Integer responses) {
        this.responses = responses;
    }

    public Integer getDropped() {
        return dropped;
    }

    public void setDropped(Integer dropped) {
        this.dropped = dropped;
    }

    @Override
    public String toString() {
        return "Statistics{" +
                "nodeName='" + nodeName + '\'' +
                ", sent=" + sent +
                ", queueSize=" + queueSize +
                ", responses=" + responses +
                ", durations=" + durations +
                ", dropped=" + dropped +
                '}';
    }
}
