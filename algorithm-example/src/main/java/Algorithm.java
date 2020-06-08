import hkube.algo.wrapper.IAlgorithm;
import hkube.api.IHKubeAPI;
import hkube.api.INode;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class Algorithm implements IAlgorithm {


    @Override
    public void Init(JSONObject args) {

    }

    @Override
    public JSONObject Start(JSONArray input, IHKubeAPI hkubeAPI) throws Exception {
        Map<String, Object> data = new HashMap<>();
        data.put("myAnswer", 33);
        data.put("mirror", input);
      //  JSONObject result =  hkubeAPI.startAlgorithm("green-alg",new JSONArray(),false);
        JSONObject simpleInput = new JSONObject();
        Map files = new HashMap();
        files.put("link","mylink");
        simpleInput.put("files",files);
      //  hkubeAPI.startStoredPipeLine("simple",simpleInput);
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
      //  hkubeAPI.startRawSubPipeLine("myRow",nodes,new JSONObject(),new HashMap(),new HashMap());
        //return new JSONObject(result);
        return simpleInput;
    }

    @Override
    public void Stop() {

    }

    @Override
    public void Cleanup() {

    }
}
