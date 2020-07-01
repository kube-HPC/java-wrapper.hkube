package hkube.algo.wrapper;

import hkube.api.IHKubeAPI;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.util.Collection;

public interface IAlgorithm {
    void Init(JSONObject args);

    JSONObject Start(Collection input, IHKubeAPI hkubeAPI) throws Exception;

    void Stop();
    void Cleanup();
}
