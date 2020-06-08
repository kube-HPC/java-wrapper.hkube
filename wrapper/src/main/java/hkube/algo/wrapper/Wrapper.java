package hkube.algo.wrapper;

import hkube.algo.CommandResponseListener;
import hkube.algo.HKubeAPIImpl;
import hkube.algo.ICommandSender;
import hkube.communication.DataServer;
import hkube.communication.zmq.ZMQServer;
import hkube.storage.IStorageConfig;
import hkube.storage.StorageFactory;
import hkube.storage.TaskStorage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.tyrus.client.ClientManager;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.websocket.*;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@ClientEndpoint
public class Wrapper implements ICommandSender {
    private final WrapperConfig mConfig;
    Session userSession = null;
    private IAlgorithm mAlgorithm;
    JSONObject mArgs;
    JSONArray mInput;
    List<CommandResponseListener> listeners = new ArrayList<>();
    HKubeAPIImpl hkubeAPI = new HKubeAPIImpl(this);
    ZMQServer zmqServer ;
    DataServer dataServer;
    TaskStorage taskResultStorage;



    private static final Logger logger = LogManager.getLogger();

    public Wrapper(IAlgorithm algorithm, WrapperConfig config) {
        mConfig = config;
        zmqServer = new ZMQServer(mConfig.commConfig);
        dataServer= new DataServer(zmqServer,mConfig.commConfig);
        mAlgorithm = algorithm;
        taskResultStorage = new StorageFactory(config.storageConfig).getTaskStorage();
        connect();
    }

    public void addResponseListener(CommandResponseListener listener) {
        listeners.add(listener);
    }

    private void connect() {
        String uriString = "ws://" + mConfig.getHost() + ":" + mConfig.getPort()+"/?storage="+mConfig.getStorageVersion()+"&encoding=" + mConfig.getStorageEncodingType();
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
        logger.info("opening websocket");
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
        logger.info("closing websocket" + error);
    }

    public void sendMessage(String command, JSONObject data) {
        logger.info("Sending message: " + command);
        Map<String, Object> toSend = new HashMap<String, Object>();
        toSend.put("command", command);
        toSend.put("data", data);
        JSONObject message = new JSONObject(toSend);
        this.userSession.getAsyncRemote().sendText(message.toString());
    }


    /**
     * Callback hook for Message Events. This method will be invoked when a client
     * send a message.
     *
     * @param message The text message
     */
    @OnMessage
    public void onMessage(String message) {
        try {
//            logger.info("message: "+message);
            JSONObject msgAsJson = new JSONObject(message);
            String command = (String) msgAsJson.get("command");
            JSONObject data = msgAsJson.optJSONObject("data");
            listeners.forEach(listener -> {
                listener.onCommand(command, data);
            });
            logger.info("got command : " + command);
            CompletableFuture.supplyAsync(() -> {
                try {
                    switch (command) {
                        case "initialize":
                            mArgs = data;
                            if (mArgs != null) {
                                mInput = mArgs.getJSONArray("input");
                            } else {
                                mInput = new JSONArray();
                            }
//                            logger.info("data: "+data);

                            mAlgorithm.Init(mArgs);
                            sendMessage("initialized", null);
                            break;
                        case "exit":
                            mAlgorithm.Cleanup();
                            sendMessage("exited", null);
                            System.exit(0);
                            break;
                        case "start":
                            sendMessage("started", null);
                            try {
                                DataAdapter dataAdapter = new DataAdapter(mConfig);
                                dataAdapter.placeData(mArgs);
                                JSONObject res = mAlgorithm.Start(mInput,hkubeAPI);
                                String taskId = (String)mArgs.get("taskId");
                                dataServer.addTaskData(taskId,res);
                                JSONObject discoveryData = new JSONObject();
                                discoveryData.put("taskId",taskId);
                                JSONObject discoveryComm = new JSONObject();
                                discoveryComm.put("host","localhost");
                                discoveryComm.put("port", mConfig.commConfig.getListeningPort());
                                sendMessage( "storing",discoveryComm );
                                taskResultStorage.put((String) mArgs.get("jobId"),taskId,res);
                                sendMessage("done",new JSONObject());
                            } catch (Exception ex) {
                                logger.error(ex);
                                Map<String, String> res = new HashMap<>();
                                res.put("code", "Failed");
                                res.put("message", ex.toString());
                                sendMessage("error", new JSONObject(res));
                            } finally {
                                mArgs = new JSONObject();
                                mInput = new JSONArray();
                            }
                            break;
                        case "stop":
                            mAlgorithm.Stop();
                            sendMessage("stopped", null);
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

    }
}
