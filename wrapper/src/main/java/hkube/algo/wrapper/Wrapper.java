package hkube.algo.wrapper;

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
    Session userSession = null;
    private IAlgorithm m_algorithm;
    JSONObject m_args;
    JSONArray m_input;
    List<CommandResponseListener> listeners = new ArrayList<>();
    HKubeAPIImpl hkubeAPI = new HKubeAPIImpl(this);



    private static final Logger logger = LogManager.getLogger();

    public Wrapper(IAlgorithm algorithm) {
        m_algorithm = algorithm;
        connect();
    }

    public void addResponseListener(CommandResponseListener listener) {
        listeners.add(listener);
    }

    private void connect() {
        String port = System.getenv("WORKER_SOCKET_PORT");
        String host = System.getenv("WORKER_SOCKET_HOST");
        if (port == null) {
            port = "3000";
        }
        if (host == null) {
            host = "localhost";
        }
        String uriString = "ws://" + host + ":" + port;
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
//                    logger.error(exc);
                }
                Thread.sleep(200);
//                logger.info("Retrying to connect to websocket");
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
        m_algorithm.Cleanup();
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
                            m_args = data;
                            if (m_args != null) {
                                m_input = m_args.getJSONArray("input");
                            } else {
                                m_input = new JSONArray();
                            }
//                            logger.info("data: "+data);

                            m_algorithm.Init(m_args);
                            sendMessage("initialized", null);
                            break;
                        case "exit":
                            m_algorithm.Cleanup();
                            sendMessage("exited", null);
                            System.exit(0);
                            break;
                        case "start":
                            sendMessage("started", null);
                            try {
                                JSONObject res = m_algorithm.Start(m_input,hkubeAPI);
                                sendMessage("done", res);
                            } catch (Exception ex) {
                                logger.error(ex);
                                Map<String, String> res = new HashMap<>();
                                res.put("code", "Failed");
                                res.put("message", ex.toString());
                                sendMessage("error", new JSONObject(res));
                            } finally {
                                m_args = new JSONObject();
                                m_input = new JSONArray();
                            }
                            break;
                        case "stop":
                            m_algorithm.Stop();
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
