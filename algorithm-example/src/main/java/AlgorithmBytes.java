import hkube.algo.wrapper.IAlgorithm;
import hkube.api.IHKubeAPI;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.util.Collection;

public class AlgorithmBytes implements IAlgorithm {


    @Override
    public void Init(JSONObject args) {

    }


    @Override
    public JSONObject Start(Collection input, IHKubeAPI hkubeAPI) throws Exception {
        JSONObject output = new JSONObject();
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


