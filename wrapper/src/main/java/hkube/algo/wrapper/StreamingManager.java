package hkube.algo.wrapper;

import hkube.algo.ICommandSender;
import hkube.communication.ICommConfig;
import hkube.communication.streaming.*;
import hkube.communication.streaming.zmq.Listener;
import hkube.communication.streaming.zmq.Producer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class StreamingManager implements IMessageListener {
    ICommandSender errorHandler;
    MessageProducer messageProducer;
    Map<String, MessageListener> messageListeners = new HashMap();
    List<IStreamingManagerMsgListener> registeredListeners = new ArrayList();


    boolean listeningToMessages = false;
    Map<String, List> parsedFlows = new HashMap();
    String defaultFlow;
    ICommConfig commConfig;
    ThreadLocal local = new ThreadLocal();

    StreamingManager(ICommandSender errorHandler, ICommConfig commConfig) {
        this.commConfig = commConfig;
        this.errorHandler = errorHandler;

    }

    public boolean isListeningToMessages() {
        return listeningToMessages;
    }

    void setParsedFlows(Map<String, List> flows, String defaultFlow) {
        this.parsedFlows = flows;
        this.defaultFlow = defaultFlow;
    }

    void sendError(Exception exc) {
        exc.printStackTrace();
        Map<String, String> res = new HashMap<>();
        res.put("code", "Failed");
        res.put("message", exc.toString());
        errorHandler.sendMessage("errorMessage", res, true);
    }


    void setupStreamingProducer(IStatisticsListener onStatistics, List nextNodes, String me) {
        Producer zmqProducer = new Producer(me, commConfig.getStreamListeningPort(), nextNodes, commConfig.getEncodingType(), commConfig.getStreamMaxBufferSize(), errorHandler);
        messageProducer = new MessageProducer(zmqProducer, commConfig, nextNodes);
        messageProducer.registerStatisticsListener(onStatistics);
        if (nextNodes.size() > 0) {
            messageProducer.start();
        }
    }


    void setupStreamingListeners(List<Map> parents, String nodeName) {
        synchronized (messageListeners) {
            parents.stream().forEach(predecessor -> {
                        Map address = (Map) predecessor.get("address");
                        String host = (String) address.get("host");
                        Integer port = (Integer) address.get("port");
                        String type = (String) predecessor.get("type");
                        String originNodeName = (String) predecessor.get("nodeName");
                        if (type.equals("Add")) {
                            Listener zmqListener = new Listener(host, String.valueOf(port), commConfig.getEncodingType(), nodeName, errorHandler);
                            MessageListener listener = new MessageListener(commConfig, zmqListener, originNodeName);
                            listener.register(this);
                            messageListeners.put(host + port, listener);
                            if (listeningToMessages) {
                                listener.start();
                            }
                        }
                        if (type.equals("Del")) {
                            MessageListener listener = messageListeners.remove(host + port);
                            if (listeningToMessages) {
                                listener.close(false);
                            }
                        }

                    }
            );
        }
    }


    public void registerInputListener(IStreamingManagerMsgListener onMessage) {
        registeredListeners.add(onMessage);
    }

    @Override
    public void onMessage(Object msg, Flow flow, String origin) {
        local.set(flow);
        registeredListeners.stream().forEach(listener -> {
            listener.onMessage(msg, origin);
        });
        local.remove();
    }

    public void startMessageListening() {
        listeningToMessages = true;
        synchronized (messageListeners) {
            messageListeners.values().stream().forEach(messageListener -> messageListener.start());
        }
    }


    public void sendMessage(Object msg, String flowName) {
        if (messageProducer == null) {
            throw new RuntimeException("Trying to send a message from a none stream pipeline or after close had been applied on algorithm");
        }

        if (messageProducer.getConsumers().size() > 0) {
            Flow flow = null;
            if (flowName == null) {
                flow = (Flow) local.get();
                if (flow == null) {
                    flowName = defaultFlow;
                    List parsedFlow = parsedFlows.get(flowName);
                    flow = new Flow(parsedFlow);
                }
            }
            messageProducer.produce(flow, msg);
        }
    }

    public void stopStreaming(boolean force) {
        if (listeningToMessages) {
            synchronized (messageListeners) {
                messageListeners.values().stream().forEach(listener -> {
                    listener.close(force);
                });
                messageListeners.clear();
            }
            listeningToMessages = false;
            registeredListeners = new ArrayList<>();
            if (messageProducer != null) {
                messageProducer.close(force);
            }
            messageProducer = null;
        }
    }

    public void clearListeners() {
        synchronized (messageListeners) {
            messageListeners.clear();
        }
    }
}