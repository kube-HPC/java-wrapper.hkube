package hkube.communication.streaming;

import hkube.communication.ICommConfig;

import hkube.encoding.EncodingManager;
import hkube.model.HeaderContentPair;
import hkube.utils.PrintUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;


public class MessageProducer {


    List<String> consumers;
    EncodingManager encoding;
    int statisticsInterval;
    public IProducer producerAdapter;
    Map durationCache = new HashMap();
    Map<String, ArrayDeque<Long>> grossDurationCache = new HashMap();

    Map<String, Integer> responseCount = new HashMap();
    Boolean active = true;
    int printStatistics = 0;
    List<IStatisticsListener> statisticsListeners = new ArrayList();
    private static final Logger logger = LogManager.getLogger();

    public MessageProducer(IProducer producerAdapter, ICommConfig config, List<String> consumerNodes) {
        consumers = consumerNodes;
        this.producerAdapter = producerAdapter;
        statisticsInterval = config.getstreamstatisticsinterval() * 1000;
        encoding = new EncodingManager(config.getEncodingType());
        config.getstreamstatisticsinterval();


        consumerNodes.stream().forEach(consumerNode ->
        {
            durationCache.put(consumerNode, new ArrayDeque());
            grossDurationCache.put(consumerNode, new ArrayDeque());
            responseCount.put(consumerNode, 0);
        });
        producerAdapter.registerResponseAccumulator(new ResponseAccumulator());
    }

    public List<String> getConsumers() {
        return consumers;
    }

    void sendStatisticsEvery() {

        Thread statisticsThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (active) {
                    sendStatistics();
                    try {
                        Thread.sleep(statisticsInterval);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        if (consumers.size() > 0) {
            statisticsThread.start();
        }
    }


    public void produce(Flow meesageFlowPattern, Object obj) {
        HeaderContentPair pair = encoding.encodeSeparately(obj);
        Message msg = new Message(pair.getContent(), pair.getHeaderAsBytes(), meesageFlowPattern);

        producerAdapter.produce(msg);
    }

    class ResponseAccumulator implements IResponseAccumulator {

        @Override
        public void onResponse(byte[] response, String origin, Long grossDuration) {
            Map decodedResponse = (Map) encoding.decodeNoHeader(response);
            responseCount.put(origin, responseCount.get(origin) + 1);
            Double duration =( (Number) decodedResponse.get("duration")).doubleValue();
            ArrayDeque<Double> durations = (ArrayDeque<Double>) durationCache.get(origin);
            ArrayDeque<Long> grossDurations = grossDurationCache.get(origin);
            durations.add(duration);
            grossDurations.add(grossDuration);
        }
    }


    ArrayDeque<Double> resetDurationCache(String consumerNode) {
        ArrayDeque<Double> durations = (ArrayDeque<Double>) durationCache.get(consumerNode);
        durationCache.put(consumerNode, new ArrayDeque<>());
        return durations;
    }

    ArrayDeque<Long> resetGrossDurationCache(String consumerNode) {
        ArrayDeque<Long> durations = grossDurationCache.get(consumerNode);
        grossDurationCache.put(consumerNode, new ArrayDeque<>());
        return durations;
    }

    public void registerStatisticsListener(IStatisticsListener listener) {
        statisticsListeners.add(listener);
    }

    void sendStatistics() {
        List<Statistics> statistics = new ArrayList<>();
        consumers.stream().forEach(consumer -> {
            int sent = producerAdapter.getSent(consumer);
            int queueSize = producerAdapter.getQueueSize(consumer);
            ArrayDeque<Double> durations = resetDurationCache(consumer);
            ArrayDeque<Long> grossDuration = resetGrossDurationCache(consumer);
            int dropped = producerAdapter.getDropped(consumer);
            int responses = responseCount.get(consumer);
            ArrayDeque queueDurations = producerAdapter.resetQueueTimeDurations(consumer);
            Statistics stats = new Statistics(consumer, sent, queueSize, grossDuration, durations, responses, dropped, queueDurations);
            statistics.add(stats);

        });
        statisticsListeners.stream().forEach(listener -> {
            listener.onStatistics(statistics);
        });
        printStatistics++;
        if (printStatistics % 10 == 0) {
            {
                Object json = new PrintUtil().toJSON(statistics);
                System.out.print(json + "\n");
            }
        }
    }

    public void start() {
        producerAdapter.start();
        sendStatisticsEvery();
    }

    public void close(boolean force) {
        producerAdapter.close(force);
        active = false;
    }
}
