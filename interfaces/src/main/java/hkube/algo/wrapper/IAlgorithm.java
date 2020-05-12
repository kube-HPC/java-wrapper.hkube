package hkube.algo.wrapper;

import org.json.JSONArray;
import org.json.JSONObject;

public interface IAlgorithm {
    void Init(JSONObject args);

    JSONObject Start(JSONArray input) throws Exception;
    void Stop();
    void Cleanup();
}
