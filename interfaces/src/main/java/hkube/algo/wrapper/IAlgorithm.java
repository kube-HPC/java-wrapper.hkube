package hkube.algo.wrapper;

import hkube.api.IHKubeAPI;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Map;

public interface IAlgorithm {
    void Init(Map args);

    Map Start(Map input, IHKubeAPI hkubeAPI) throws Exception;

    void Stop();
    void Cleanup();
}
