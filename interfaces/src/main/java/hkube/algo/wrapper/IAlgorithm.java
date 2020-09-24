package hkube.algo.wrapper;

import hkube.api.IHKubeAPI;
import java.util.Map;

public interface IAlgorithm {
    void Init(Map args);

    Object Start(Map input, IHKubeAPI hkubeAPI) throws Exception;

    void Stop();
    void Cleanup();
}
