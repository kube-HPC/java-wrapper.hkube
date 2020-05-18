package hkube.algo.wrapper;

import hkube.api.IHKubeAPI;
import org.json.JSONArray;
import org.json.JSONObject;

public interface IAlgorithm {
    void Init(JSONObject args);

    JSONObject Start(JSONArray input, IHKubeAPI hkubeAPI) throws Exception;
    void Stop();
    void Cleanup();
}
