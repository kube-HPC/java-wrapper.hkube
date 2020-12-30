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
    String port;
    Double maxMemorySize;
    EncodingManager encoding;
    int statisticsInterval;
    IProducer producerAdapter;
    Map responsesCache = new HashMap();
    Map<String, Integer> responseCount = new HashMap();
    Boolean active = true;
    int printStatistics = 0;
    List<IStatisticsListener> listeners = new ArrayList();
    private static final Logger logger = LogManager.getLogger();

    public MessageProducer(IProducer producerAdapter, ICommConfig config, List<String> consumerNodes) {
        consumers = consumerNodes;
        this.producerAdapter = producerAdapter;
        maxMemorySize = config.getStreamMaxBufferSize() * 1024d * 1024;
        statisticsInterval = config.getstreamstatisticsinterval()*1000;
        encoding = new EncodingManager(config.getEncodingType());
        config.getstreamstatisticsinterval();


        consumerNodes.stream().forEach(consumerNode -> {
            responsesCache.put(consumerNode, new ArrayDeque());
            responseCount.put(consumerNode, 0);
        });
        producerAdapter.registerResponseAccumulator(new ResponseAccumulator());
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
        public void onResponse(byte[] response, String origin) {
            Map decodedResponse = (Map) encoding.decodeNoHeader(response);
            responseCount.put(origin, responseCount.get(origin) + 1);
            Double duration = (Double) decodedResponse.get("duration");
            ArrayDeque<Double> durations = (ArrayDeque<Double>) responsesCache.get(origin);
            durations.add(duration);
        }
    }


    ArrayDeque<Double> resetResponseCache(String consumerNode) {
        ArrayDeque<Double> durations = (ArrayDeque<Double>) responsesCache.get(consumerNode);
        responsesCache.put(consumerNode, new ArrayDeque<Float>());
        return durations;
    }


    public void registerStatisticsListener(IStatisticsListener listener) {
        listeners.add(listener);
    }

    void sendStatistics() {
        List<Statistics> statistics = new ArrayList<>();
        consumers.stream().forEach(consumer -> {
            int sent = producerAdapter.getSent(consumer);
            int queueSize = producerAdapter.getQueueSize(consumer);
            ArrayDeque<Double> durations = resetResponseCache(consumer);
            int dropped = producerAdapter.getDropped(consumer);
            int responses = responseCount.get(consumer);
            Statistics stats = new Statistics(consumer, sent, queueSize, durations,responses, dropped);
            statistics.add(stats);

        });
        listeners.stream().forEach(listener -> {
            listener.onStatistics(statistics);
        });
        printStatistics++;
        if (printStatistics % 10 == 0) {
            {
                Object json = new PrintUtil().toJSON(statistics);
                System.out.print(json+"\n");
            }
        }
    }

    public void start() {
        producerAdapter.start();
        sendStatisticsEvery();
    }
}
