package hkube.communication.streaming;

import java.util.List;

public interface IStatisticsListener {
    void onStatistics(List<Statistics> statistics);
}
