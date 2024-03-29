package hkube.algo;

import hkube.algo.wrapper.DataAdapter;
import hkube.algo.wrapper.IContext;
import hkube.algo.wrapper.StreamingManager;
import hkube.api.IHKubeAPI;
import hkube.api.INode;
import hkube.communication.streaming.IStreamingManagerMsgListener;
import hkube.consts.messages.Outgoing;
import hkube.utils.PrintUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class HKubeAPIImpl implements IHKubeAPI, CommandResponseListener {
    private static final Logger logger = LogManager.getLogger();
    int lastExcution = 0;
    ICommandSender commandSender;
    Map<String, APIExecutionFuture> executions = new HashMap();
    StreamingManager streamingManager;

    DataAdapter dataAdapter;
    IContext context;
    boolean isDebug = false;

    public HKubeAPIImpl(ICommandSender sender, IContext context, DataAdapter dataAdapter, StreamingManager streamingManager, boolean isDebug) {
        this.dataAdapter = dataAdapter;
        this.commandSender = sender;
        this.streamingManager = streamingManager;
        this.context = context;
        sender.addResponseListener(this);
        this.isDebug = isDebug;
    }

    @Override
    public Future<Object> startAlgorithmAsynch(String name, List input, boolean resultAsRaw) {
        APIExecutionFuture future = new APIExecutionFuture();
        String executionId = getExecutionId(future);
        Map data = new HashMap();
        data.put(Consts.execId, executionId);
        data.put(Consts.algorithmName, name);
        Map<String, Map> storage = dataAdapter.setAlgoData(input, context.getJobId());
        List storageInput = storage.keySet().stream().map(dataIdentifier -> "$$" + dataIdentifier).collect(Collectors.toList());
        data.put(Consts.storage, storage);
        data.put(Consts.storageInput, storageInput);
        data.put(Consts.resultAsRaw, resultAsRaw);
        commandSender.sendMessage(Consts.startAlgorithmExecution, data, false);
        return future;
    }

    @Override
    public Object startAlgorithm(String name, List input, boolean resultAsRaw) {
        APIExecutionFuture future = (APIExecutionFuture) startAlgorithmAsynch(name, input, resultAsRaw);
        return returnWhenExecDone(future);
    }

    @Override
    public Future<Object> startStoredPipeLineAsynch(String name, Map flowInput) {
        return startStoredPipeLineAsynch(name, flowInput, true);
    }

    @Override
    public Future<Object> startStoredPipeLineAsynch(String name, Map flowInput, boolean includeResult) {
        APIExecutionFuture future = new APIExecutionFuture();
        String executionId = getExecutionId(future);
        Map data = new HashMap();
        data.put(Consts.subPipelineId, executionId);
        data.put(Consts.includeResult, includeResult);
        Map subPipeline = new HashMap();
        subPipeline.put("name", name);
        subPipeline.put(Consts.flowInput, flowInput);
        data.put(Consts.subPipeline, subPipeline);
        commandSender.sendMessage("startStoredSubPipeline", data, false);
        return future;
    }

    @Override
    public Object startStoredPipeLine(String name, Map flowInput) {
        APIExecutionFuture future = (APIExecutionFuture) startStoredPipeLineAsynch(name, flowInput);
        return returnWhenExecDone(future);
    }

    @Override
    public Object startStoredPipeLine(String name, Map flowInput, boolean includeResult) {
        APIExecutionFuture future = (APIExecutionFuture) startStoredPipeLineAsynch(name, flowInput, includeResult);
        return returnWhenExecDone(future);
    }

    @Override
    public Future<Object> startRawSubPipeLineAsynch(String name, INode[] nodes, Map flowInput, Map options, Map webhooks) {
        APIExecutionFuture future = new APIExecutionFuture();
        String executionId;
        executionId = getExecutionId(future);
        Map data = new HashMap();
        data.put("subPipelineId", executionId);
        List nodesList = new ArrayList();
        Arrays.asList(nodes).forEach(node -> {
            Map nodeAttributes = new HashMap();
            nodeAttributes.put("nodeName", node.getName());
            nodeAttributes.put("algorithmName", node.getAlgorithmName());
            nodeAttributes.put("input", node.getInput());
            nodesList.add(nodeAttributes);
        });

        Map subPipeline = new HashMap();
        subPipeline.put("name", name);
        subPipeline.put("flowInput", flowInput);
        subPipeline.put("nodes", nodesList);
        subPipeline.put("options", options);
        subPipeline.put("webhooks", webhooks);
        data.put("subPipeline", subPipeline);
        commandSender.sendMessage("startRawSubPipeline", data, false);
        return future;
    }

    @Override
    public Object startRawSubPipeLine(String name, INode[] nodes, Map flowInput, Map options, Map webhooks) {
        APIExecutionFuture future = (APIExecutionFuture) startRawSubPipeLineAsynch(name, nodes, flowInput, options, webhooks);
        return returnWhenExecDone(future);
    }

    String getExecutionId(APIExecutionFuture future) {
        String executionId = String.valueOf(++lastExcution);
        executions.put(executionId, future);
        return executionId;
    }

    Object returnWhenExecDone(APIExecutionFuture future) {
        while (!future.isDone()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void onCommand(String command, Object data, boolean isDebug) {
        String[] executionCommands = {"algorithmExecutionDone", "algorithmExecutionError"};
        String[] subPipeCommands = {"subPipelineDone",
                "subPipelineError", "subPipelineStopped"};
        logger.debug("got command" + command);
        if (data != null) {
            logger.debug(new PrintUtil().getAsJsonStr(data));
        } else {
            logger.debug("data null");
        }
        if (Arrays.asList(executionCommands).contains(command)) {
            String executionId = (String) ((Map) data).get("execId");
            if (!isDebug) {
                Map results = (Map) ((Map) data).get("response");
                Object res = dataAdapter.getData(results, null);
                ((Map) data).put("response", res);
            }
            executions.get(executionId).setResult(((Map) data));
        }
        if (Arrays.asList(subPipeCommands).contains(command)) {
            String executionId = (String) ((Map) data).get("subPipelineId");
            //Support getting
            if (!isDebug) {
                Map results = (Map) ((Map) data).get("response");
                Object res = "No results";
                if (results != null) {
                    res = dataAdapter.getData(results, null);

                }
                ((Map) data).put("response", res);
            }
            executions.get(executionId).setResult((((Map) data).get("response")));
        }
        logger.debug("Execution result" + data);
    }

    public void registerInputListener(IStreamingManagerMsgListener onMessage) {
        streamingManager.registerInputListener(onMessage);
    }

    public Map<String, Object> getStreamingStatistics() {
        if (streamingManager.getMessageProducer() != null) {
            return streamingManager.getMessageProducer().getStatistics();
        } else {
            Map singleNodeMap = new HashMap();
            singleNodeMap.put("nodeName", "NextNodeName");
            singleNodeMap.put("sent", -1);
            singleNodeMap.put("queueSize", -1);
            singleNodeMap.put("dropped", -1);
            ArrayList nodeList = new ArrayList();
            nodeList.add(singleNodeMap);
            Map mockMap = new HashMap();
            mockMap.put("binarySize", -1);
            mockMap.put("configuredMaxBinarySize", -1);
            mockMap.put("statisticsPerNode", nodeList);
            return  mockMap;
        }
    }

    public void startMessageListening() {
        streamingManager.startMessageListening();
    }

    public void sendMessage(Object msg, String flowName) {
        if (isDebug) {
            Map data = new HashMap();
            data.put("message", msg);
            data.put("flowName", flowName);
            data.put("sendMessageId", streamingManager.getCurrentSendMessageId());
            commandSender.sendMessage(Outgoing.streamingOutMessage, data, false);
        } else {
            streamingManager.sendMessage(msg, flowName);
        }
    }

    public void sendMessage(Object msg) {
        sendMessage(msg, null);
    }

    @Override
    public void startSpan(String name, Map tags) {
        Map data = new HashMap();
        data.put("name", name);
        data.put("tags", tags);
        commandSender.sendMessage(Outgoing.startSpan, data, false);
    }

    @Override
    public void finishSpan(Map tags) {
        Map data = new HashMap();
        data.put(tags, tags);
        commandSender.sendMessage(Outgoing.finishSpan, data, false);
    }

    public void stopStreaming(boolean force) {
        streamingManager.stopStreaming(force);
    }

    public boolean isListeningToMessages() {
        return streamingManager.isListeningToMessages();
    }

}
