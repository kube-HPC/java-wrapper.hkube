package hkube.algo;

import hkube.api.IHKubeAPI;
import hkube.api.INode;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class HKubeAPIImpl implements IHKubeAPI, CommandResponseListener {
    private static final Logger logger = LogManager.getLogger();
    int lastExcution = 0;
    ICommandSender commandSender;
    Map<String, APIExecutionFuture> executions = new HashMap();

    public HKubeAPIImpl(ICommandSender sender) {
        this.commandSender = sender;
        sender.addResponseListener(this);
    }

    @Override
    public Future<JSONObject> startAlgorithmAsynch(String name, JSONArray input, boolean resultAsRaw) {
        APIExecutionFuture future = new APIExecutionFuture();
        String executionId = getExecutionId(future);
        JSONObject data = new JSONObject();
        data.put(Consts.execId, executionId);
        data.put(Consts.algorithmName, name);
        data.put(Consts.input, input);
        data.put(Consts.resultAsRaw, resultAsRaw);
        commandSender.sendMessage(Consts.startAlgorithmExecution, data);
        return future;
    }

    @Override
    public JSONObject startAlgorithm(String name, JSONArray input, boolean resultAsRaw) {
        APIExecutionFuture future = (APIExecutionFuture) startAlgorithmAsynch(name, input, resultAsRaw);
        return returnWhenExecDone(future);
    }

    @Override
    public Future<JSONObject> startStoredPipeLineAsynch(String name, JSONObject flowInput) {
        APIExecutionFuture future = new APIExecutionFuture();
        String executionId = getExecutionId(future);
        JSONObject data = new JSONObject();
        data.put(Consts.subPipelineId, executionId);
        Map subPipeline = new HashMap();
        subPipeline.put("name", name);
        subPipeline.put(Consts.flowInput, flowInput);
        data.put(Consts.subPipeline, subPipeline);
        commandSender.sendMessage("startStoredSubPipeline", data);
        return future;
    }

    @Override
    public JSONObject startStoredPipeLine(String name, JSONObject flowInput) {
        APIExecutionFuture future = (APIExecutionFuture) startStoredPipeLineAsynch(name, flowInput);
        return returnWhenExecDone(future);
    }

    @Override
    public Future<JSONObject> startRawSubPipeLineAsynch(String name, INode[] nodes, JSONObject flowInput, Map options, Map webhooks) {
        APIExecutionFuture future = new APIExecutionFuture();
        String executionId;
        executionId = getExecutionId(future);
        JSONObject data = new JSONObject();
        data.put("subPipelineId", executionId);
        List nodesList = new ArrayList();
        Arrays.asList(nodes).forEach(node -> {
            Map nodeAttributes = new HashMap();
            nodeAttributes.put("nodeName",node.getName());
            nodeAttributes.put("algorithmName",node.getAlgorithmName());
            nodeAttributes.put("input",node.getInput());
            nodesList.add(nodeAttributes);
        });

        Map subPipeline = new HashMap();
        subPipeline.put("name", name);
        subPipeline.put("flowInput", flowInput);
        subPipeline.put("nodes", nodesList);
        subPipeline.put("options", options);
        subPipeline.put("webhooks", webhooks);
        data.put("subPipeline", subPipeline);
        commandSender.sendMessage("startRawSubPipeline", data);
        return future;
    }

    @Override
    public JSONObject startRawSubPipeLine(String name, INode[] nodes, JSONObject flowInput, Map options, Map webhooks) {
        APIExecutionFuture future = (APIExecutionFuture) startRawSubPipeLineAsynch(name, nodes, flowInput, options, webhooks);
        return returnWhenExecDone(future);
    }

    String getExecutionId(APIExecutionFuture future) {
        String executionId = String.valueOf(++lastExcution);
        executions.put(executionId, future);
        return executionId;
    }

    JSONObject returnWhenExecDone(APIExecutionFuture future) {
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
    public void onCommand(String command, JSONObject data) {
        String[] executionCommands = {"algorithmExecutionDone", "algorithmExecutionError"};
        String[] subPipeCommands = {"subPipelineDone",
                "subPipelineError", "subPipelineStopped"};
        if (Arrays.asList(executionCommands).contains(command)) {
            String executionId = (String) data.get("execId");
            executions.get(executionId).setResult(data);
            logger.info(data);
        }
        if (Arrays.asList(subPipeCommands).contains(command)) {
            String executionId = (String) data.get("subPipelineId");
            executions.get(executionId).setResult(data);
            logger.info(data);
        }
    }
}
