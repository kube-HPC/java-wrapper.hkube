package hkube.communication.streaming;

import java.util.ArrayDeque;

public class Statistics {
    String nodeName;
    ArrayDeque<Double> netDurations;
    Integer sent;
    Integer queueSize;
    Integer responses;
    ArrayDeque<Long> durations;
    Integer dropped;


    ArrayDeque<Long> queueDurations;




    public Statistics(String nodeName, Integer sent, Integer queueSize, ArrayDeque<Long> duration, ArrayDeque<Double> netDurations, Integer responses, Integer dropped, ArrayDeque<Long> queueDurations) {
        this.nodeName = nodeName;
        this.sent = sent;
        this.queueSize = queueSize;
        this.responses = responses;
        this.durations = duration;
        this.dropped = dropped;
        this.netDurations = netDurations;
        this.queueDurations = queueDurations;
    }
    public ArrayDeque<Long> getQueueDurations() {
        return queueDurations;
    }

    public void setQueueDurations(ArrayDeque<Long> queueDurations) {
        this.queueDurations = queueDurations;
    }

    public ArrayDeque<Double> getNetDurations() {
        return netDurations;
    }

    public void setNetDurations(ArrayDeque<Double> netDurations) {
        this.netDurations = netDurations;
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

    public ArrayDeque<Long> getDurations() {
        return durations;
    }

    public void setDurations(ArrayDeque<Long> durations) {
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
                ", netDurations=" + netDurations +
                ", dropped=" + dropped +
                ", durations=" + durations +
                "}";
    }
}
