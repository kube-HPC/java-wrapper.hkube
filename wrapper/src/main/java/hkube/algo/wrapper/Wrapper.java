package hkube.algo.wrapper;

import hkube.algo.CommandResponseListener;
import hkube.algo.HKubeAPIImpl;
import hkube.algo.ICommandSender;
import hkube.communication.DataServer;
import hkube.communication.zmq.ZMQServer;
import hkube.encoding.EncodingManager;
import hkube.storage.StorageFactory;
import hkube.storage.TaskStorage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.tyrus.client.ClientManager;
import org.json.JSONObject;

import javax.websocket.*;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@ClientEndpoint
public class Wrapper implements ICommandSender {
    private final WrapperConfig mConfig;
    private static boolean isDebugMode = false;
    Session userSession = null;
    private IAlgorithm mAlgorithm;
    Map mArgs;

    List<CommandResponseListener> listeners = new ArrayList<>();
    HKubeAPIImpl hkubeAPI;
    ZMQServer zmqServer;
    DataServer dataServer;
    TaskStorage taskResultStorage;
    DataAdapter dataAdapter;
    EncodingManager workerEncoder;

    private static final Logger logger = LogManager.getLogger();

    public Wrapper(IAlgorithm algorithm, WrapperConfig config) {
        mConfig = config;
        dataAdapter = new DataAdapter(mConfig);
        hkubeAPI = new HKubeAPIImpl(this, dataAdapter);
        zmqServer = new ZMQServer(mConfig.commConfig);
        dataServer = new DataServer(zmqServer, mConfig.commConfig);
        mAlgorithm = algorithm;
        taskResultStorage = new StorageFactory(config.storageConfig).getTaskStorage();
        workerEncoder = new EncodingManager(mConfig.getEncodingType());
        connect();
    }

    public static void setDebugMode() {
        isDebugMode=true;
    }

    public void addResponseListener(CommandResponseListener listener) {
        listeners.add(listener);
    }

    private void connect() {

        String uriString;
        uriString = mConfig.getUrl();
        if (uriString == null) {
            uriString = "ws://" + mConfig.getHost() + ":" + mConfig.getPort() + "/?storage=" + mConfig.getStorageVersion() + "&encoding=" + mConfig.getEncodingType();
        }
        else {
            uriString = uriString+"?encoding="+ mConfig.getEncodingType();
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
                    logger.error(exc);
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
        System.exit(-1);
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
        logger.info("Sending message to worker: " + command);
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

    private void onMessage(Map msgAsMap) {
        try {
            String command = (String) msgAsMap.get("command");
            Map data = (Map)msgAsMap.get("data");
            listeners.forEach(listener -> {
                listener.onCommand(command, data);
            });
            logger.info("got message from worker:" + command);
            CompletableFuture.supplyAsync(() -> {
                try {
                    switch (command) {
                        case "initialize":
                            mArgs = data;
                            mAlgorithm.Init(mArgs);
                            sendMessage("initialized", null, false);
                            break;
                        case "exit":
                            mAlgorithm.Cleanup();
                            sendMessage("exited", null, false);
                            System.exit(0);
                            break;
                        case "start":
                            sendMessage("started", null, false);
                            Collection input;
                            try {
                                logger.debug("Before fetching input data");
                                input = dataAdapter.placeData(mArgs);
                                mArgs.put("input",input);
                                logger.debug("After fetching input data");
                                if (logger.isDebugEnabled()) {
                                    logger.debug("input data after decoding " + input);
                                }
                                logger.debug("Before running algorithm");
                                Object res;
                                res = mAlgorithm.Start( mArgs, hkubeAPI);
                                logger.debug("After running algorithm");
                                String taskId = (String) mArgs.get("taskId");
                                String jobId = (String) mArgs.get("jobId");
                                dataServer.addTaskData(taskId, res);
                                Collection savePaths =(Collection) ((Map)mArgs.get("info")).get("savePaths");
                                Map metaData = dataAdapter.getMetadata(savePaths, res);
                                byte [] encodedData = dataAdapter.encode(res, mConfig.commConfig.getEncodingType());
                                int resEncodedSize = encodedData.length;
                                Map resultStoringInfo = dataAdapter.getStoringInfo(mConfig, jobId, taskId, metaData, resEncodedSize);
                                if(logger.isDebugEnabled()){
                                    logger.debug("result storing data" + resultStoringInfo);
                                }
                                if(!isDebugMode) {
                                    sendMessage("storing", resultStoringInfo, false);
                                    taskResultStorage.putEncoded((String) mArgs.get("jobId"), taskId, encodedData);
                                    sendMessage("done", new HashMap(), false);
                                }else{
                                    sendMessage("done",res,false);
                                }
                            } catch (Exception ex) {
                                logger.error("unexpected exception", ex);
                                Map<String, String> res = new HashMap<>();
                                res.put("code", "Failed");
                                res.put("message", ex.toString());
                                sendMessage("errorMessage", res, true);
                            } finally {
                                mArgs = new HashMap();
                            }
                            break;
                        case "stop":
                            mAlgorithm.Stop();
                            sendMessage("stopped", null, false);
                            break;
                        default:
                            logger.info("got command: " + command);

                    }
                } catch (Exception exc) {
                    logger.error(exc);
                }
                return null;
            });
        } catch (Exception exc) {
            logger.error(exc);
        }
    }


    private void onExit() {
        logger.warn("exiting");
    }
}
