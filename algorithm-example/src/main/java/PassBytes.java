import hkube.algo.wrapper.IAlgorithm;
import hkube.api.IHKubeAPI;


import java.util.Collection;
import java.util.Map;

public class PassBytes implements IAlgorithm {


    @Override
    public void Init(Map args) {

    }

    @Override
    public Object Start(Map args, IHKubeAPI hkubeAPI) throws Exception {
        byte[] buffer = (byte[]) ((Collection) args.get("input")).iterator().next();
        return buffer;
    }

    @Override
    public void Stop() {

    }

    @Override
    public void Cleanup() {

    }
}
