import hkube.algo.wrapper.IAlgorithm;
import hkube.api.IHKubeAPI;
import hkube.api.INode;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class AlgorithmBytes  implements IAlgorithm{



        @Override
        public void Init(JSONObject args) {

        }

        @Override
        public JSONObject Start(JSONArray input, IHKubeAPI hkubeAPI) throws Exception {
            JSONObject output = new JSONObject();
            output.put("prevInput",input);
            byte[] myBytes = new byte[20000];
            myBytes[4]=5;
            output.put("myBytes",myBytes);
            return output;
        }

        @Override
        public void Stop() {

        }

        @Override
        public void Cleanup() {

        }
    }


