import hkube.algo.CommandResponseListener;
import hkube.algo.Consts;
import hkube.algo.HKubeAPIImpl;
import hkube.algo.ICommandSender;
import hkube.api.INode;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;


public class HKubeAPITest {
    @Test
    public void testStartStoredPipeLine() {
        HKubeAPIImpl api = new HKubeAPIImpl(new ICommandSender() {
            CommandResponseListener listener;

            @Override
            public void sendMessage(String command, JSONObject data, boolean isError) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Map result = new HashMap();
                        result.put(Consts.subPipelineId, data.get(Consts.subPipelineId));
                        result.put("storedResult", "5");
                        listener.onCommand(Consts.subPipelineDone, new JSONObject(result));
                    }
                }).start();
            }

            @Override
            public void addResponseListener(CommandResponseListener listener) {
                this.listener = listener;
            }
        });
        JSONObject result = api.startStoredPipeLine("pipeName", new JSONObject());
        assert result.get("storedResult") == "5";
    }

    @Test
    public void testStartRawPipeLine() {
        HKubeAPIImpl api = new HKubeAPIImpl(new ICommandSender() {
            CommandResponseListener listener;

            @Override
            public void sendMessage(String command, JSONObject data, boolean isError) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Map result = new HashMap();
                        result.put(Consts.subPipelineId, data.get(Consts.subPipelineId));
                        result.put("rawResult", "2");
                        listener.onCommand(Consts.subPipelineDone, new JSONObject(result));
                    }
                }).start();
            }

            @Override
            public void addResponseListener(CommandResponseListener listener) {
                this.listener = listener;
            }
        });
        JSONObject result = api.startRawSubPipeLine("pipeName", new INode[]{}, new JSONObject(), null, null);
        assert result.get("rawResult") == "2";
    }
    @Test
    public void asyncTest() throws ExecutionException, InterruptedException {
        HKubeAPIImpl api = new HKubeAPIImpl(new ICommandSender() {
            CommandResponseListener listener;

            @Override
            public void sendMessage(String command, JSONObject data, boolean isError) {
                if (command.equals(Consts.startRawSubPipeline)) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Map result = new HashMap();
                            result.put(Consts.subPipelineId, data.get(Consts.subPipelineId));
                            result.put("rawResult", "2");
                            listener.onCommand(Consts.subPipelineDone, new JSONObject(result));
                        }
                    }).start();
                }
                if (command.equals(Consts.startAlgorithmExecution)) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Map result = new HashMap();
                            result.put(Consts.execId, data.get(Consts.execId));
                            result.put("algoResult", "3");
                            listener.onCommand(Consts.algorithmExecutionDone, new JSONObject(result));
                        }
                    }).start();
                }
            }

            @Override
            public void addResponseListener(CommandResponseListener listener) {
                this.listener = listener;
            }
        });
        Future rawResult = api.startRawSubPipeLineAsynch("pipeName", new INode[]{}, new JSONObject(), null, null);
        Future algoReslut = api.startAlgorithmAsynch("algName",new JSONArray(),false);
        while(!rawResult.isDone()) Thread.sleep(200);
        JSONObject result = (JSONObject) rawResult.get();
        assert result.get("rawResult") == "2";
        while(!algoReslut.isDone()) Thread.sleep(200);
        result = (JSONObject) algoReslut.get();
        assert result.get("algoResult") == "3";

    }
}
