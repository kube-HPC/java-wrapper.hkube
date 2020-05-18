package hkube.api;


import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;
import java.util.concurrent.Future;

public interface IHKubeAPI {
    public Future<JSONObject>  startAlgorithmAsynch(String name, JSONArray input, boolean resultAsRaw);
    public JSONObject startAlgorithm(String name, JSONArray input, boolean resultAsRaw);
    public Future<JSONObject> startStoredPipeLineAsynch(String name,JSONObject flowInput);
    public JSONObject startStoredPipeLine(String name, JSONObject flowInput);
    public Future<JSONObject> startRawSubPipeLineAsynch(String name,INode[] nodes,JSONObject flowInput,Map options,Map webhooks);
    public JSONObject startRawSubPipeLine(String name, INode[] nodes, JSONObject flowInput, Map options, Map webhooks);
}

