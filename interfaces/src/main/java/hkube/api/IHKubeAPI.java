package hkube.api;


import hkube.communication.streaming.IStreamingManagerMsgListener;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

public interface IHKubeAPI {
    public Future<Map> startAlgorithmAsynch(String name, List input, boolean resultAsRaw);

    public Map startAlgorithm(String name, List input, boolean resultAsRaw);

    public Future<Map> startStoredPipeLineAsynch(String name, Map flowInput);

    public Future<Map> startStoredPipeLineAsynch(String name, Map flowInput, boolean includeResult);

    public Map startStoredPipeLine(String name, Map flowInput);

    public Map startStoredPipeLine(String name, Map flowInput, boolean includeResult);

    public Future<Map> startRawSubPipeLineAsynch(String name, INode[] nodes, Map flowInput, Map options, Map webhooks);

    public Map startRawSubPipeLine(String name, INode[] nodes, Map flowInput, Map options, Map webhooks);

    void registerInputListener(IStreamingManagerMsgListener onMessage);

    void startMessageListening();

    void sendMessage(Object msg, String flowName);
    void sendMessage(Object msg);

    void stopStreaming(boolean force);

    boolean isListeningToMessages();
}

