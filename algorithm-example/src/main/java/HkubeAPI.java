import hkube.algo.wrapper.IAlgorithm;
import hkube.api.IHKubeAPI;
import hkube.api.INode;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class HkubeAPI implements IAlgorithm {


    @Override
    public void Init(Map args) {

    }

    @Override
    public Map Start(Map input, IHKubeAPI hkubeAPI) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("myAnswer", 33);
        data.put("mirror", input);
        ArrayList jsonArray = new ArrayList();
        jsonArray.add(data);
        Map result = hkubeAPI.startAlgorithm("green-alg", jsonArray, false);
        Map simpleInput = new HashMap();
        Map files = new HashMap();
        files.put("link", "thislink");
        files.put("other", "otherValue");
        simpleInput.put("files", files);
        Map stroedResult = hkubeAPI.startStoredPipeLine("simple", simpleInput);
        INode node = new INode() {
            @Override
            public String getName() {
                return "yellow-alg-NOde";
            }

            @Override
            public JSONObject[] getInput() {
                return new JSONObject[0];
            }

            @Override
            public void setInput(JSONObject[] input) {

            }

            @Override
            public String getAlgorithmName() {
                return "yellow-alg";
            }

            @Override
            public void setAlgorithmName(String algorithmName) {

            }
        };
        INode[] nodes = {node};
        Map raw = hkubeAPI.startRawSubPipeLine("myRaw", nodes, new HashMap(), new HashMap(), new HashMap());
        Map algResult = new HashMap<>();
        algResult.put("storedResult", stroedResult);
        algResult.put("algo-green-result", result);
        algResult.put("rawResult", raw);
        return algResult;
    }

    @Override
    public void Stop() {

    }

    @Override
    public void Cleanup() {

    }
}
