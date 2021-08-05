package hkube.api;


import hkube.communication.streaming.IStreamingManagerMsgListener;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

public interface IHKubeAPI {
    public Future<Object> startAlgorithmAsynch(String name, List input, boolean resultAsRaw);

    public Object startAlgorithm(String name, List input, boolean resultAsRaw);

    public Future<Object> startStoredPipeLineAsynch(String name, Map flowInput);

    public Future<Object> startStoredPipeLineAsynch(String name, Map flowInput, boolean includeResult);

    public Object startStoredPipeLine(String name, Map flowInput);

    public Object startStoredPipeLine(String name, Map flowInput, boolean includeResult);

    public Future<Object> startRawSubPipeLineAsynch(String name, INode[] nodes, Map flowInput, Map options, Map webhooks);

    public Object startRawSubPipeLine(String name, INode[] nodes, Map flowInput, Map options, Map webhooks);

    void registerInputListener(IStreamingManagerMsgListener onMessage);

    void startMessageListening();

    void sendMessage(Object msg, String flowName);
    void sendMessage(Object msg);

    void startSpan(String name,Map tags);
    void finishSpan(Map tags);

    void stopStreaming(boolean force);

    boolean isListeningToMessages();
}

