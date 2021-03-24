package hkube.communication.streaming;

import hkube.communication.ICommConfig;
import hkube.encoding.EncodingManager;
import hkube.model.Header;

import java.util.*;

public class MessageListener {
    String messageOriginNodeName;
    EncodingManager encodingManager;

    public IListener getListenerAdapter() {
        return listenerAdapter;
    }

    IListener listenerAdapter;
    List<IMessageListener> messageListeners= new ArrayList<>();


    public MessageListener(ICommConfig config, IListener listenerAdapter, String messageOriginNodeName) {
        this.messageOriginNodeName = messageOriginNodeName;
        encodingManager = new EncodingManager(config.getEncodingType());
        this.listenerAdapter = listenerAdapter;
        this.listenerAdapter.setMessageHandler(new MessageHandler());
    }
    public void register(IMessageListener listener){
        messageListeners.add(listener);
    }

    class MessageHandler implements IMessageHandler {


        @Override
        public byte[] onMessage(Message message) {
            long start = new Date().getTime();
            Object decoded = encodingManager.decodeSeparately(new Header(message.getHeader()), message.getData());
            messageListeners.stream().forEach(
                    listener -> {
                        listener.onMessage(decoded,message.getFlow(), messageOriginNodeName);
                    }
            );
            long end = new Date().getTime();
            Map result = new HashMap();
            result.put("duration", (double) (end - start));
            return encodingManager.encodeNoHeader(result);
        }
    }
    public void ready(boolean isReady){
        listenerAdapter.ready(isReady);

    }
    public void start() {
        System.out.print("Starting listener to " + this.messageOriginNodeName);
        listenerAdapter.start();

    }
    public void close(boolean force) {
        listenerAdapter.close(false);

    }
}
