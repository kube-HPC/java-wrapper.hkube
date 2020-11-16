package hkube.algo;

import hkube.algo.wrapper.DataAdapter;
import hkube.api.IHKubeAPI;
import hkube.api.INode;

import hkube.utils.PrintUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class HKubeAPIImpl implements IHKubeAPI, CommandResponseListener {
    private static final Logger logger = LogManager.getLogger();
    int lastExcution = 0;
    ICommandSender commandSender;
    Map<String, APIExecutionFuture> executions = new HashMap();

    DataAdapter dataAdapter;

    public HKubeAPIImpl(ICommandSender sender, DataAdapter dataAdapter) {
        this.dataAdapter = dataAdapter;
        this.commandSender = sender;
        sender.addResponseListener(this);
    }

    @Override
    public Future<Map> startAlgorithmAsynch(String name, List input, boolean resultAsRaw) {
        APIExecutionFuture future = new APIExecutionFuture();
        String executionId = getExecutionId(future);
        Map data = new HashMap();
        data.put(Consts.execId, executionId);
        data.put(Consts.algorithmName, name);
        data.put(Consts.input, input);
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
    public void onCommand(String command, Map data, boolean isDebug) {
        String[] executionCommands = {"algorithmExecutionDone", "algorithmExecutionError"};
        String[] subPipeCommands = {"subPipelineDone",
                "subPipelineError", "subPipelineStopped"};
        logger.debug("got command" + command) ;
        if (data != null) {
            logger.debug(new PrintUtil().getAsJsonStr(data));
        } else {
            logger.debug("data null");
        }
        if (Arrays.asList(executionCommands).contains(command)) {
            String executionId = (String) data.get("execId");
            if (!isDebug) {
                Map results = (Map) data.get("response");
                Object res = dataAdapter.getData(results, null);
                data.put("response", res);
            }
            executions.get(executionId).setResult(data);
        }
        if (Arrays.asList(subPipeCommands).contains(command)) {
            String executionId = (String) data.get("subPipelineId");
            //Support getting
            if (!isDebug) {
                Map results = (Map) data.get("response");
                Object res = "No results";
                if (results != null) {
                    res = dataAdapter.getData(results, null);
                }
                data.put("response", res);
            }
            executions.get(executionId).setResult(data);
        }
        logger.debug("Execution result" + data);
    }
}
