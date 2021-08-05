package hkube.algo.wrapper;

import hkube.algo.CommandResponseListener;
import hkube.algo.HKubeAPIImpl;
import hkube.algo.ICommandSender;
import hkube.caching.Cache;
import hkube.communication.DataServer;
import hkube.communication.IRequestListener;
import hkube.communication.IRequestServer;
import hkube.communication.streaming.IStatisticsListener;
import hkube.communication.streaming.Statistics;
import hkube.communication.zmq.RequestFactory;
import hkube.communication.zmq.ZMQServer;
import hkube.consts.messages.Outgoing;
import hkube.encoding.EncodingManager;
import hkube.model.HeaderContentPair;
import hkube.storage.StorageFactory;
import hkube.storage.TaskStorage;
import hkube.consts.messages.Incomming;
import hkube.utils.IPrinter;
import hkube.utils.PrintStreamInterceptor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.tyrus.client.ClientManager;
import org.json.JSONObject;

import javax.websocket.*;
import java.io.PrintStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.CompletableFuture;


@ClientEndpoint
public class Wrapper implements ICommandSender, IContext {
    private final WrapperConfig mConfig;
    private static boolean isDebugMode = false;
    Session userSession = null;
    private IAlgorithm mAlgorithm;
    private IAlgorithm originalAlg;
    private IAlgorithm statelessAlg;
    Map mArgs;

    List<CommandResponseListener> listeners = new ArrayList<>();
    HKubeAPIImpl hkubeAPI;
    IRequestServer zmqServer;
    DataServer dataServer;
    TaskStorage taskResultStorage;
    DataAdapter dataAdapter;
    EncodingManager workerEncoder;
    StreamingManager streamingManager;
    String nodeName;
    boolean stopping;
    PrintStreamInterceptor interceptor;


    private static final Logger logger = LogManager.getLogger();
    private PrintStream originalOutput;

    public Wrapper(IAlgorithm algorithm, WrapperConfig config) {
        System.out.println("IP is " + System.getenv("POD_IP"));
        Cache.init(config.commConfig.getMaxCacheSize());
        mConfig = config;
        dataAdapter = new DataAdapter(mConfig, new RequestFactory());
        streamingManager = new StreamingManager(this, config.commConfig);
        hkubeAPI = new HKubeAPIImpl(this, this, dataAdapter, streamingManager, isDebugMode);
        if (!isDebugMode) {
            zmqServer = new ZMQServer(mConfig.commConfig);
        } else {
            zmqServer = createMockZMQServer();
        }
        dataServer = new DataServer(zmqServer, mConfig.commConfig);
        originalAlg = algorithm;
        mAlgorithm = originalAlg;
        statelessAlg = new StatelessWrapper(algorithm);
        taskResultStorage = new StorageFactory(config.storageConfig).getTaskStorage();
        workerEncoder = new EncodingManager(mConfig.getEncodingType());
        connect();
        originalOutput = System.out.printf("");//Access out, so it won't be null
        interceptor = new PrintStreamInterceptor(originalOutput, new IPrinter() {
            @Override
            public void print(String data) {
                sendMessage(Outgoing.logData,  new String []{data}, false);
            }
        });
    }

    public static void setDebugMode() {
        isDebugMode = true;
    }

    public void addResponseListener(CommandResponseListener listener) {
        listeners.add(listener);
    }

    private void connect() {

        String uriString;
        uriString = mConfig.getUrl();
        if (uriString == null) {
            uriString = "ws://" + mConfig.getHost() + ":" + mConfig.getPort() + "/?storage=" + mConfig.getStorageVersion() + "&encoding=" + mConfig.getEncodingType();
        } else {
            uriString = uriString + "?encoding=" + mConfig.getEncodingType();
        }
        try {
            logger.info("connecting to uri: " + uriString);

            URI uri = new URI(uriString);

            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            ClientManager clientManager = (ClientManager) container;
            clientManager.getProperties().put("org.glassfish.tyrus.incomingBufferSize", 150000000);
            container.setAsyncSendTimeout(Long.MAX_VALUE);
            container.setDefaultMaxSessionIdleTimeout(Long.MAX_VALUE);
            container.setDefaultMaxBinaryMessageBufferSize(Integer.MAX_VALUE);
            container.setDefaultMaxTextMessageBufferSize(Integer.MAX_VALUE);

            while (this.userSession == null) {
                try {
                    container.connectToServer(this, uri);
                } catch (Exception exc) {
                    logger.debug(exc);
                }
                Thread.sleep(200);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Callback hook for Connection open events.
     *
     * @param userSession the userSession which is opened.
     */
    @OnOpen
    public void onOpen(Session userSession) {
        logger.info("connected to worker");
        this.userSession = userSession;
        this.userSession.setMaxIdleTimeout(Long.MAX_VALUE);
    }

    /**
     * Callback hook for Connection close events.
     *
     * @param userSession the userSession which is getting closed.
     * @param reason      the reason for connection close
     */
    @OnClose
    public void onClose(Session userSession, CloseReason reason) {
        logger.info("websocket closed with reason :" + reason);
        this.userSession = null;
        mAlgorithm.Cleanup();
        if (reason.getCloseCode().getCode() == 1013) {
            logger.error("Another user is already connected");
            System.exit(-1);
        }
//        System.exit(-1);
    }

    /**
     * Callback hook for Connection close events.
     *
     * @param userSession the userSession which is getting closed.
     * @param error       error
     */
    @OnError
    public void onError(Session userSession, Throwable error) {
        logger.error("closing websocket" + error);
    }

    public void sendMessage(String command, Object data, boolean isError) {
        Map<String, Object> toSend = new HashMap();
        toSend.put("command", command);
        if (isError) {
            toSend.put("error", data);
        } else {
            toSend.put("data", data);
        }
        JSONObject message = new JSONObject(toSend);
        if (workerEncoder.getName().equals("json")) {
            this.userSession.getAsyncRemote().sendText(message.toString());
        } else {
            Map root = new HashMap();
            root.put("data", toSend);
            byte[] bytes = workerEncoder.encodeNoHeader(root);
            ByteBuffer buffer = ByteBuffer.wrap(bytes);
            this.userSession.getAsyncRemote().sendBinary(buffer);
            logger.info(command + " sent");
        }
    }

    @OnMessage
    public void onMessage(String message) {
        Map msgAsJson = (Map) workerEncoder.decodeNoHeader(message.getBytes());
        onMessage(msgAsJson);
    }

    /**
     * Callback hook for Message Events. This method will be invoked when a client
     * send a message.
     *
     * @param message The text message
     */
    @OnMessage
    public void onMessage(byte[] message) {

        Map msgAsJson = (Map) ((Map) workerEncoder.decodeNoHeader(message)).get("data");
        onMessage(msgAsJson);

    }

    private boolean isStreaming() {
        return (mArgs != null && mArgs.get("kind") != null && mArgs.get("kind").equals("stream"));
    }

    public String getJobId() {
        return (String) mArgs.get("jobId");
    }

    private void onMessage(Map msgAsMap) {
        try {

            String command = (String) msgAsMap.get("command");
            Object data = msgAsMap.get("data");
            listeners.forEach(listener -> {
                logger.debug("got command " + command);
                listener.onCommand(command, data, isDebugMode);
            });
            logger.info("got message from worker:" + command);
            CompletableFuture.supplyAsync(() -> {
                try {
                    switch (command) {
                        case "initialize":
                            mArgs = ((Map) data);
                            nodeName = (String) ((Map) data).get("nodeName");
                            if (isStreaming() && "stateless".equals(mArgs.get("stateType"))) {
                                mAlgorithm = statelessAlg;
                            }
                            if (isDebugMode) {
                                System. setOut(interceptor);
                            }
                            mAlgorithm.Init(mArgs);
                            if (isDebugMode) {
                                System. setOut(originalOutput);
                            }
                            logger.info("Sending initialized");
                            sendMessage("initialized", null, false);
                            break;
                        case "exit":
                            mAlgorithm.Cleanup();
                            logger.info("Sending exited");
                            sendMessage("exited", null, false);
                            if (isStreaming()) {
                                if (streamingManager.messageProducer != null) {
                                    logger.warn("Number of messages lef in queue on exit:" + streamingManager.messageProducer.producerAdapter.getQueueSize());
                                }
                            }
                            System.exit(0);
                            break;
                        case "start":
                            if (isStreaming()) {
                                streamingManager.clearListeners();
                                if (((List) mArgs.get("childs")).size() > 0) {
                                    setupStreamingProducer();
                                }
                            }
                            logger.info("Sending started");
                            sendMessage("started", null, false);
                            Collection input;
                            try {
                                logger.debug("Before fetching input data");
                                input = dataAdapter.placeData(mArgs);
                                mArgs.put("input", input);
                                logger.debug("After fetching input data");
                                if (logger.isDebugEnabled()) {
                                    logger.debug("input data after decoding " + input);
                                }
                                logger.debug("fBefore running algorithm");
                                Object res;
                                if (isDebugMode) {
                                    System. setOut(interceptor);
                                }
                                res = mAlgorithm.Start(mArgs, hkubeAPI);
                                if (isDebugMode) {
                                    System. setOut(originalOutput);
                                }
                                logger.debug("After running algorithm");
                                String taskId = (String) mArgs.get("taskId");
                                String jobId = (String) mArgs.get("jobId");

                                Collection savePaths = (Collection) ((Map) mArgs.get("info")).get("savePaths");
                                Map metaData = dataAdapter.getMetadata(savePaths, res);
                                HeaderContentPair encodedData = dataAdapter.encode(res, mConfig.commConfig.getEncodingType());
                                boolean dataAdded = dataServer.addTaskData(taskId, encodedData);
                                int resEncodedSize = encodedData.getContent().length;
                                Map resultStoringInfo = dataAdapter.getStoringInfo(jobId, taskId, metaData, resEncodedSize);
                                if (logger.isDebugEnabled()) {
                                    logger.debug("result storing data" + resultStoringInfo);
                                }
                                if (!isDebugMode) {
                                    logger.info("Sending storeing");
                                    if (dataAdded) {
                                        sendMessage("storing", resultStoringInfo, false);
                                        taskResultStorage.put((String) mArgs.get("jobId"), taskId, encodedData);
                                    } else {
                                        taskResultStorage.put((String) mArgs.get("jobId"), taskId, encodedData);
                                        sendMessage("storing", resultStoringInfo, false);
                                    }
                                    if (isStreaming()) {
                                        streamingManager.stopStreaming(false);
                                    }
                                    sendMessage("done", new HashMap(), false);
                                } else {
                                    sendMessage("done", res, false);
                                }
                            } catch (Exception ex) {
                                logger.error("unexpected exception", ex);
                                Map<String, String> res = new HashMap<>();
                                res.put("code", "Failed");
                                res.put("message", ex.toString());
                                onError(res);
                            } finally {
                                mArgs = new HashMap();
                            }
                            break;
                        case "stop":
                            mAlgorithm.Stop();
                            if (isStreaming()) {
                                if (stopping) {
                                    logger.warn("Got command stop while stopping");
                                } else {
                                    stopping = true;
                                    boolean forceStop = (boolean) ((Map) data).get("forceStop");
                                    Thread stoppingThread = new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            while (stopping) {
                                                sendMessage("stopping", null, false);
                                                try {
                                                    Thread.sleep(1000);
                                                } catch (InterruptedException e) {
                                                    logger.error(e);
                                                }
                                            }
                                            logger.info("Done stopping");
                                        }
                                    });
                                    if (!forceStop) {

                                        stoppingThread.start();
                                    }
                                    streamingManager.stopStreaming(forceStop);
                                    stopping = false;
                                    stoppingThread.join();
                                }
                            }

                            sendMessage("stopped", null, false);
                            break;
                        case Incomming.serviceDiscoveryUpdate:
                            discoveryUpdate((List) data);
                        case Incomming.streamingInMessage:
                            String origin = (String) ((Map) data).get("origin");
                            Map msg = (Map) ((Map) data).get("payload");
                            String sendMessageId = (String) ((Map) data).get("sendMessageId");

                            streamingManager.onMessage(msg, origin, sendMessageId);
                            Map sendMessagIdMap = new HashMap();
                            sendMessagIdMap.put("sendMessageId", sendMessageId);
                            sendMessage(Outgoing.streamingInMessageDone, sendMessagIdMap, false);
                        default:
                            logger.info("got command: " + command);

                    }
                } catch (Exception exc) {
                    logger.error("unexpected exception", exc);
                    Map<String, String> res = new HashMap<>();
                    res.put("code", "Failed");
                    res.put("message", exc.toString());
                    onError(res);
                    logger.error(exc);
                }
                return null;
            });
        } catch (Exception exc) {
            logger.error("unexpected exception", exc);
            Map<String, String> res = new HashMap<>();
            res.put("code", "Failed");
            res.put("message", exc.toString());
            onError(res);
            logger.error(exc);
        }
    }

    void setupStreamingProducer() {
        Map parsedFlows = (Map) mArgs.get("parsedFlow");
        String defaultFlow = (String) mArgs.get("defaultFlow");
        streamingManager.setParsedFlows(parsedFlows, defaultFlow);

        streamingManager.setupStreamingProducer(new IStatisticsListener() {
            @Override
            public void onStatistics(List<Statistics> statistics) {
                sendMessage("streamingStatistics", statistics, false);
            }
        }, (List) mArgs.get("childs"), nodeName);
    }

    void discoveryUpdate(List discovery) {
        logger.info("Got discovery update " + discovery);
        streamingManager.setupStreamingListeners(discovery, nodeName);
    }

    public void onError(Object data) {
        if (isStreaming()) {
            streamingManager.stopStreaming(true);
        }
        sendMessage("errorMessage", data, true);
    }

    private void onExit() {
        logger.warn("exiting");
    }

    private IRequestServer createMockZMQServer() {
        return new IRequestServer() {
            @Override
            public void addRequestsListener(IRequestListener listener) {

            }

            @Override
            public void reply(List<HeaderContentPair> replies) {

            }

            @Override
            public void close() {

            }
        };
    }

}
