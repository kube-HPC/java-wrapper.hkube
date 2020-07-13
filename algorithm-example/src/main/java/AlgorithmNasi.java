import hkube.algo.wrapper.IAlgorithm;
import hkube.api.IHKubeAPI;

import java.util.Map;
public class AlgorithmNasi implements IAlgorithm {
    @Override
    public void Init(Map map) {
    }
    @Override
    public Object Start(Map options, IHKubeAPI hkubeAPI) throws Exception {
        Object input = options.get("input");
        return input;
    }
    @Override
    public void Stop() {
    }
    @Override
    public void Cleanup() {
    }
}