package hkube.api;


import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;
import java.util.concurrent.Future;

public interface IHKubeAPI {
    public Future<Map>  startAlgorithmAsynch(String name, JSONArray input, boolean resultAsRaw);
    public Map startAlgorithm(String name, JSONArray input, boolean resultAsRaw);
    public Future<Map> startStoredPipeLineAsynch(String name,JSONObject flowInput);
    public Map startStoredPipeLine(String name, JSONObject flowInput);
    public Future<Map> startRawSubPipeLineAsynch(String name,INode[] nodes,JSONObject flowInput,Map options,Map webhooks);
    public Map startRawSubPipeLine(String name, INode[] nodes, JSONObject flowInput, Map options, Map webhooks);
}

