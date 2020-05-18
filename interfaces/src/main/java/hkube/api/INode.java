package hkube.api;


import org.json.JSONObject;

public interface INode {
    public String getName();
    public JSONObject[] getInput();
    public void setInput(JSONObject[] input);
    public String getAlgorithmName();
    public void setAlgorithmName(String algorithmName);
}
