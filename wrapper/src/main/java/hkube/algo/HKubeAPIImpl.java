package hkube.algo;

import hkube.algo.wrapper.DataAdapter;
import hkube.algo.wrapper.IContext;
import hkube.algo.wrapper.StreamingManager;
import hkube.api.IHKubeAPI;
import hkube.api.INode;
import hkube.communication.streaming.IStreamingManagerMsgListener;
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

    public HKubeAPIImpl(ICommandSender sender, IContext context, DataAdapter dataAdapter, StreamingManager streamingManager) {
        this.dataAdapter = dataAdapter;
        this.commandSender = sender;
        this.streamingManager = streamingManager;
        this.context = context;
        sender.addResponseListener(this);
    }

    @Override
    public Future<Map> startAlgorithmAsynch(String name, List input, boolean resultAsRaw) {
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
    public Map startAlgorithm(String name, List input, boolean resultAsRaw) {
        APIExecutionFuture future = (APIExecutionFuture) startAlgorithmAsynch(name, input, resultAsRaw);
        return returnWhenExecDone(future);
    }

    @Override
    public Future<Map> startStoredPipeLineAsynch(String name, Map flowInput) {
        return startStoredPipeLineAsynch(name, flowInput, true);
    }

    @Override
    public Future<Map> startStoredPipeLineAsynch(String name, Map flowInput, boolean includeResult) {
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
    public Map startStoredPipeLine(String name, Map flowInput) {
        APIExecutionFuture future = (APIExecutionFuture) startStoredPipeLineAsynch(name, flowInput);
        return returnWhenExecDone(future);
    }

    @Override
    public Map startStoredPipeLine(String name, Map flowInput, boolean includeResult) {
        APIExecutionFuture future = (APIExecutionFuture) startStoredPipeLineAsynch(name, flowInput, includeResult);
        return returnWhenExecDone(future);
    }

    @Override
    public Future<Map> startRawSubPipeLineAsynch(String name, INode[] nodes, Map flowInput, Map options, Map webhooks) {
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
    public Map startRawSubPipeLine(String name, INode[] nodes, Map flowInput, Map options, Map webhooks) {
        APIExecutionFuture future = (APIExecutionFuture) startRawSubPipeLineAsynch(name, nodes, flowInput, options, webhooks);
        return returnWhenExecDone(future);
    }

    String getExecutionId(APIExecutionFuture future) {
        String executionId = String.valueOf(++lastExcution);
        executions.put(executionId, future);
        return executionId;
    }

    Map returnWhenExecDone(APIExecutionFuture future) {
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
            executions.get(executionId).setResult(((Map) data));
        }
        logger.debug("Execution result" + data);
    }

    public void registerInputListener(IStreamingManagerMsgListener onMessage) {
        streamingManager.registerInputListener(onMessage);
    }

    public void startMessageListening() {
        streamingManager.startMessageListening();
    }

    public void sendMessage(Object msg, String flowName) {
        streamingManager.sendMessage(msg, flowName);
    }

    public void sendMessage(Object msg) {
        streamingManager.sendMessage(msg, null);
    }

    public void stopStreaming(boolean force) {
        streamingManager.stopStreaming(force);
    }

    public boolean isListeningToMessages() {
        return streamingManager.isListeningToMessages();
    }

}
