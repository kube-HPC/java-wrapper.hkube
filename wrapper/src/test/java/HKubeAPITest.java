import hkube.algo.CommandResponseListener;
import hkube.algo.Consts;
import hkube.algo.HKubeAPIImpl;
import hkube.algo.ICommandSender;
import hkube.algo.wrapper.DataAdapter;
import hkube.algo.wrapper.IContext;
import hkube.algo.wrapper.WrapperConfig;
import hkube.api.INode;
import hkube.communication.zmq.RequestFactory;
import org.junit.Test;

import java.util.ArrayList;
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
            public void sendMessage(String command, Object data, boolean isError) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Map dataMap = (Map) data;
                        Map result = new HashMap();
                        result.put(Consts.subPipelineId, dataMap.get(Consts.subPipelineId));
                        Map storedResult = new HashMap();
                        result.put("response", storedResult);
                        storedResult.put("storedResult", "5");
                        listener.onCommand(Consts.subPipelineDone, result, false);
                    }
                }).start();
            }

            @Override
            public void addResponseListener(CommandResponseListener listener) {
                this.listener = listener;
            }
        }, new IContext() {
            @Override
            public String getJobId() {
                return null;
            }
        }, new DataAdapter(new WrapperConfig(), new RequestFactory()) {
            @Override
            public Object getData(Map single, String jobId) {
                return single;
            }
        }, null,false);
        Object result = api.startStoredPipeLine("pipeName", new HashMap());
        assert ((Map) ((Map)result).get("response")).get("storedResult") == "5";
    }

    @Test
    public void testStartRawPipeLine() {
        HKubeAPIImpl api = new HKubeAPIImpl(new ICommandSender() {
            CommandResponseListener listener;

            @Override
            public void sendMessage(String command, Object data, boolean isError) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Map dataMap = (Map) data;
                        Map result = new HashMap();
                        result.put(Consts.subPipelineId, dataMap.get(Consts.subPipelineId));
                        Map rawResult = new HashMap();
                        rawResult.put("rawResult", "2");
                        result.put("response", rawResult);
                        listener.onCommand(Consts.subPipelineDone, result, false);
                    }
                }).start();
            }

            @Override
            public void addResponseListener(CommandResponseListener listener) {
                this.listener = listener;
            }
        }, new IContext() {
            @Override
            public String getJobId() {
                return null;
            }
        } ,new DataAdapter(new WrapperConfig(), new RequestFactory()) {
            @Override
            public Object getData(Map single, String jobId) {
                return single;
            }
        },null,false);

        Map result = (Map)api.startRawSubPipeLine("pipeName", new INode[]{}, new HashMap(), null, null);
        assert ((Map) result.get("response")).get("rawResult") == "2";
    }

    @Test
    public void asyncTest() throws ExecutionException, InterruptedException {
        HKubeAPIImpl api = new HKubeAPIImpl(new ICommandSender() {
            CommandResponseListener listener;

            @Override
            public void sendMessage(String command, Object data, boolean isError) {
                if (command.equals(Consts.startRawSubPipeline)) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Map dataMap = (Map) data;
                            Map result = new HashMap();
                            result.put(Consts.subPipelineId, dataMap.get(Consts.subPipelineId));
                            Map rawResult = new HashMap();
                            result.put("response", rawResult);
                            rawResult.put("rawResult", "2");
                            listener.onCommand(Consts.subPipelineDone, result, false);
                        }
                    }).start();
                }
                if (command.equals(Consts.startAlgorithmExecution)) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            Map dataMap = (Map) data;
                            Map result = new HashMap();
                            result.put(Consts.execId, dataMap.get(Consts.execId));
                            Map algoResult = new HashMap();
                            result.put("response", algoResult);
                            algoResult.put("algoResult", "3");
                            listener.onCommand(Consts.algorithmExecutionDone, result, false);
                        }
                    }).start();
                }
            }

            @Override
            public void addResponseListener(CommandResponseListener listener) {
                this.listener = listener;
            }
        }, new IContext() {
            @Override
            public String getJobId() {
                return null;
            }
        }, new DataAdapter(new WrapperConfig(), new RequestFactory()) {
            @Override
            public Object getData(Map single, String jobId) {
                return single;
            }
        }, null, false);
        Future rawResult = api.startRawSubPipeLineAsynch("pipeName", new INode[]{}, new HashMap(), null, null);
        Future algoReslut = api.startAlgorithmAsynch("algName", new ArrayList(), false);
        while (!rawResult.isDone()) Thread.sleep(200);
        Map result = (Map) rawResult.get();
        assert ((Map) result.get("response")).get("rawResult") == "2";
        while (!algoReslut.isDone()) Thread.sleep(200);
        result = (Map) algoReslut.get();
        assert ((Map) result.get("response")).get("algoResult") == "3";

    }
}
