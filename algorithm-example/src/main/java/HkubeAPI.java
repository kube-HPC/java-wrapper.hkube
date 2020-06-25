import hkube.algo.wrapper.IAlgorithm;
import hkube.api.IHKubeAPI;
import hkube.api.INode;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class HkubeAPI implements IAlgorithm {


    @Override
    public void Init(JSONObject args) {

    }

    @Override
    public JSONObject Start(JSONArray input, IHKubeAPI hkubeAPI) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("myAnswer", 33);
        data.put("mirror", input);
        JSONArray jsonArray = new JSONArray();
        jsonArray.put(0,data);
        JSONObject result =  hkubeAPI.startAlgorithm("green-alg",jsonArray,false);
        JSONObject simpleInput = new JSONObject();
        Map files = new HashMap();
        files.put("link","thislink");
        files.put("other","otherValue");
        simpleInput.put("files",files);
        JSONObject stroedResult =  hkubeAPI.startStoredPipeLine("simple",simpleInput);
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
        INode[] nodes ={node};
       JSONObject raw =  hkubeAPI.startRawSubPipeLine("myRaw",nodes,new JSONObject(),new HashMap(),new HashMap());
        JSONObject algResult = new JSONObject();
        algResult.put("storedResult",stroedResult);
        algResult.put("algo-green-result",result);
        algResult.put("rawResult",raw);
        return algResult;
    }

    @Override
    public void Stop() {

    }

    @Override
    public void Cleanup() {

    }
}
