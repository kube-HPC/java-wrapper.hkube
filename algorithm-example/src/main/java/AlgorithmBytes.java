import hkube.algo.wrapper.IAlgorithm;
import hkube.api.IHKubeAPI;

import java.util.HashMap;
import java.util.Map;

public class AlgorithmBytes implements IAlgorithm {


    @Override
    public void Init(Map args) {

    }


    @Override
    public Map Start(Map input, IHKubeAPI hkubeAPI) throws Exception {
        Map output = new HashMap();
        output.put("prevInput", input);
        byte[] myBytes = new byte[20000];
        myBytes[4] = 5;
        output.put("myBytes", myBytes);
        return output;
    }

    @Override
    public void Stop() {

    }

    @Override
    public void Cleanup() {

    }
}


