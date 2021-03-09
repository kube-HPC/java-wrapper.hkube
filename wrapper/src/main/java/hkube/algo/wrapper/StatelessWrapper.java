package hkube.algo.wrapper;

import hkube.api.IHKubeAPI;
import hkube.communication.streaming.IStreamingManagerMsgListener;
import hkube.communication.streaming.Message;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatelessWrapper implements IAlgorithm {
    boolean active = false;
    Exception exec;
    IHKubeAPI api;
    IAlgorithm originalAlgorithm;
    Map options;
    private static final Logger logger = LogManager.getLogger();
    public StatelessWrapper(IAlgorithm originalAlg) {
        originalAlgorithm = originalAlg;
    }

    @Override
    public void Init(Map args) {
        options = args;
    }

    @Override
    public Object Start(Map input, IHKubeAPI hkubeAPI) throws Exception {
        api = hkubeAPI;
        hkubeAPI.registerInputListener(new IStreamingManagerMsgListener() {
            @Override
            public void onMessage(Object msg, String origin) {
                invokeAlgorithm(msg, origin);
            }
        });
        active = true;
        hkubeAPI.startMessageListening();
        while (active) {
            if (exec != null) {
                throw exec;
            }
            Thread.sleep(1);
        }
        return null;
    }


    void invokeAlgorithm(Object msg, String origin) {
        Map args = new HashMap(options);
        HashMap streamingInput =  new HashMap();
        streamingInput.put("message",msg);
        streamingInput.put("origin",origin);
        args.put("streamInput", streamingInput);
        originalAlgorithm.Init(args);

        try {
            Object result = originalAlgorithm.Start(args, api);
            if (options.get("childs") != null && ((List)options.get("childs")).size() >0)  {
                api.sendMessage(result);
            }
        } catch (Exception e) {
            this.exec = e;

        }
    }

    @Override
    public void Stop() {
        logger.info("Stopping stateless");
        active = false;
        originalAlgorithm.Stop();
    }

    @Override
    public void Cleanup() {
        active = false;
        originalAlgorithm.Stop();
    }
}
