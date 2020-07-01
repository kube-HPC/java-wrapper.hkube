import hkube.algo.wrapper.IAlgorithm;
import hkube.api.IHKubeAPI;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Date;

public class InputOutWhen implements IAlgorithm {

    private static final Logger logger = LogManager.getLogger();
    @Override
    public void Init(JSONObject args) {
        if(logger.isDebugEnabled()) {
            logger.debug(args.toString());
        }
    }

    @Override
    public JSONObject Start(Collection input, IHKubeAPI hkubeAPI) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug(input.toString());
        }
        JSONObject output = new JSONObject();
        output.put("prevInput", new Date().toString());
        String index0 = null;
        try {if(input.size()>0)
            index0 = input.iterator().next().toString();
        } catch (Exception e) {

        }
        if (input.size()>1 && index0 != null && index0.equals("b")) {
            Integer numberOfbytes = Integer.valueOf(input.iterator().next().toString());
            byte[] myBytes = new byte[numberOfbytes];
            output.put("addedBytes", myBytes);
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
