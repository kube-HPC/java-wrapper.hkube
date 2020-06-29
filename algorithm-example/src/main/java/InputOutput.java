import hkube.algo.wrapper.IAlgorithm;
import hkube.api.IHKubeAPI;
import org.json.JSONArray;
import org.json.JSONObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class InputOutput implements IAlgorithm {

    private static final Logger logger = LogManager.getLogger();
    @Override
    public void Init(JSONObject args) {
        logger.info(args.toString());
    }

    @Override
    public JSONObject Start(JSONArray input, IHKubeAPI hkubeAPI) throws Exception {
        logger.info(input.toString());
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
