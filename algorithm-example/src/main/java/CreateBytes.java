import hkube.algo.wrapper.IAlgorithm;
import hkube.api.IHKubeAPI;
import hkube.api.INode;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class CreateBytes implements IAlgorithm {


    @Override
    public void Init(Map args) {

    }

    @Override
    public Object Start(Map input, IHKubeAPI hkubeAPI) throws Exception {
        byte[] bytesArr = new byte[20];
        bytesArr[5]=5;
        return bytesArr;
    }

    @Override
    public void Stop() {

    }

    @Override
    public void Cleanup() {

    }
}
