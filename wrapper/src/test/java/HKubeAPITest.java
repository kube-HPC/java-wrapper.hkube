import hkube.algo.CommandResponseListener;
import hkube.algo.Consts;
import hkube.algo.HKubeAPIImpl;
import hkube.algo.ICommandSender;
import hkube.algo.wrapper.DataAdapter;
import hkube.algo.wrapper.WrapperConfig;
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
            public void sendMessage(String command, Map data, boolean isError) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Map result = new HashMap();
                        result.put(Consts.subPipelineId, data.get(Consts.subPipelineId));
                        Map storedResult =  new HashMap();
                        result.put("response", storedResult);
                        storedResult.put("storedResult", "5");
                        listener.onCommand(Consts.subPipelineDone, result);
                    }
                }).start();
            }

            @Override
            public void addResponseListener(CommandResponseListener listener) {
                this.listener = listener;
            }
        },new DataAdapter(new WrapperConfig()){
            @Override
            public Object getData(Map single, String jobId) {
                return single;
            }
        });
        Map result = api.startStoredPipeLine("pipeName", new JSONObject());
        assert ((Map)result.get("response")).get("storedResult") == "5";
    }

    @Test
    public void testStartRawPipeLine() {
        HKubeAPIImpl api = new HKubeAPIImpl(new ICommandSender() {
            CommandResponseListener listener;

            @Override
            public void sendMessage(String command, Map data, boolean isError) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Map result = new HashMap();
                        result.put(Consts.subPipelineId, data.get(Consts.subPipelineId));
                        Map rawResult =  new HashMap();
                        rawResult.put("rawResult", "2");
                        result.put("response",rawResult);
                        listener.onCommand(Consts.subPipelineDone, result);
                    }
                }).start();
            }

            @Override
            public void addResponseListener(CommandResponseListener listener) {
                this.listener = listener;
            }
        },new DataAdapter(new WrapperConfig()){
            @Override
            public Object getData(Map single, String jobId) {
                return single;
            }
        });

        Map result = api.startRawSubPipeLine("pipeName", new INode[]{}, new JSONObject(), null, null);
        assert ((Map)result.get("response")).get("rawResult") == "2";
    }
    @Test
    public void asyncTest() throws ExecutionException, InterruptedException {
        HKubeAPIImpl api = new HKubeAPIImpl(new ICommandSender() {
            CommandResponseListener listener;

            @Override
            public void sendMessage(String command, Map data, boolean isError) {
                if (command.equals(Consts.startRawSubPipeline)) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Map result = new HashMap();
                            result.put(Consts.subPipelineId, data.get(Consts.subPipelineId));
                            Map rawResult =  new HashMap();
                            result.put("response", rawResult);
                            rawResult.put("rawResult", "2");
                            listener.onCommand(Consts.subPipelineDone, result);
                        }
                    }).start();
                }
                if (command.equals(Consts.startAlgorithmExecution)) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Map result = new HashMap();
                            result.put(Consts.execId, data.get(Consts.execId));
                            Map algoResult =  new HashMap();
                            result.put("response", algoResult);
                            algoResult.put("algoResult", "3");
                            listener.onCommand(Consts.algorithmExecutionDone,result);
                        }
                    }).start();
                }
            }

            @Override
            public void addResponseListener(CommandResponseListener listener) {
                this.listener = listener;
            }
        },new DataAdapter(new WrapperConfig()){
            @Override
            public Object getData(Map single, String jobId) {
                return single;
            }
        });
        Future rawResult = api.startRawSubPipeLineAsynch("pipeName", new INode[]{}, new JSONObject(), null, null);
        Future algoReslut = api.startAlgorithmAsynch("algName",new JSONArray(),false);
        while(!rawResult.isDone()) Thread.sleep(200);
        Map result = (Map) rawResult.get();
        assert ((Map)result.get("response")).get("rawResult") == "2";
        while(!algoReslut.isDone()) Thread.sleep(200);
        result = (Map) algoReslut.get();
        assert ((Map)result.get("response")).get("algoResult") == "3";

    }
}
