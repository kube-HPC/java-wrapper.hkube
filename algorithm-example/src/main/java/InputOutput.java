import hkube.algo.wrapper.IAlgorithm;
import hkube.api.IHKubeAPI;
import hkube.api.INode;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class InputOutput implements IAlgorithm {


    @Override
    public void Init(JSONObject args) {

    }

    @Override
    public JSONObject Start(JSONArray input, IHKubeAPI hkubeAPI) throws Exception {
        JSONObject output = new JSONObject();
        output.put("prevInput",input);
        String index0 = null;
        try {
            index0 = input.getString(0);
        }
        catch (Exception e){

        }
        if (index0 !=null && index0.equals("b")){
             Integer numberOfbytes =  input.getInt(1);
             byte[] myBytes = new byte[numberOfbytes];
             output.put("addedBytes",myBytes);
        }
        return output;

    }

    @Override
    public void Stop() {

    }

    @Override
    public void Cleanup() {

    }
}
